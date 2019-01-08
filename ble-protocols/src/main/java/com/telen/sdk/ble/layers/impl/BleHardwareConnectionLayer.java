package com.telen.sdk.ble.layers.impl;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.polidea.rxandroidble2.Timeout;
import com.telen.sdk.ble.GattCacheRefresherOperation;
import com.telen.sdk.ble.exceptions.ConnectionNotEstablished;
import com.telen.sdk.ble.models.ResponseType;
import com.telen.sdk.common.layers.HardwareLayerInterface;
import com.telen.sdk.common.models.Device;
import com.telen.sdk.common.models.Request;
import com.telen.sdk.common.models.Response;
import com.telen.sdk.common.utils.BytesUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class BleHardwareConnectionLayer implements HardwareLayerInterface {

    private Context mContext;
    private RxBleClient rxBleClient;
    @Nullable private BluetoothAdapter mBluetoothAdapter;

    private Map<Device, RxBleDevice> bleDevices = new HashMap<>();
    private Map<Device, CompositeDisposable> devicesDisposable = new HashMap<>();
    private Map<Device, RxBleConnection> devicesConnection = new HashMap<>();

    private Disposable scanDisposable;
    private static final String TAG = BleHardwareConnectionLayer.class.getSimpleName();

    public BleHardwareConnectionLayer(RxBleClient rxBleClient, BluetoothAdapter bluetoothAdapter, Context context) {
        this.rxBleClient = rxBleClient;
        this.mContext = context;
        this.mBluetoothAdapter = bluetoothAdapter;
    }

    @Override
    public Completable connect(Device device, boolean createBond) {
        return Completable.create(emitter -> {
            if(device==null)
                emitter.onError(new IllegalArgumentException("Cannot connect to a null device"));
            else
                emitter.onComplete();
        })
                .andThen(checkNotAlreadyConnected(device))
                .flatMapCompletable(alreadyConnected -> {
                    if(alreadyConnected)
                        return Completable.complete();
                    else
                        return connectInternal(device, createBond);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io());
    }

    @Override
    public Completable disconnect(Device device) {
        return Completable.create(emitter -> {
//                try {
            disposeDeviceConnections(device);
            devicesDisposable.remove(device);
//                }
//                catch (UndeliverableException e) {
//                    Log.e(TAG, "UndeliverableException: message="+e.getMessage());
//                }
            RxBleConnection bleConnection =  devicesConnection.remove(device);
            emitter.onComplete();
        });
    }

    @Override
    public Single<String> sendCommand(Device device, Request request, String command) {
        return sendCommand(device, request, BytesUtils.hexStringToByteArray(command))
                .observeOn(Schedulers.io())
                .doOnError(throwable -> Log.e(TAG,"sendCommand",throwable))
                .doOnSuccess(response -> Log.d(TAG,"sendCommand: response="+response));
    }

    @Override
    public Single<String> sendCommand(Device device, Request request, byte[] command) {
        return Single.create((SingleOnSubscribe<String>) emitter -> {
            RxBleConnection bleConnection = devicesConnection.get(device);
            if(bleConnection == null) {
                emitter.onError(new Exception("No existing connection to this device"));
            }
            else {
                bleConnection.writeCharacteristic(UUID.fromString(request.getCharacteristic()), command)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe(new SingleObserver<byte[]>() {
                            @Override
                            public void onSubscribe(Disposable d) {

                            }

                            @Override
                            public void onSuccess(byte[] bytes) {
                                emitter.onSuccess(BytesUtils.byteArrayToHex(bytes));
                            }

                            @Override
                            public void onError(Throwable e) {
                                emitter.onError(e);
                            }
                        });
            }
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Observable<String> listenResponses(final Device device, Response response) {
        return Observable.create((ObservableOnSubscribe<Observable<Observable<byte[]>>>)  emitter -> {
            RxBleConnection bleConnection = devicesConnection.get(device);
            if (bleConnection == null) {
                emitter.onError(new ConnectionNotEstablished("Connection is null, cannot listen for response frames"));
            } else {
                ResponseType type = ResponseType.notification;
                try {
                    if(response.getType()!=null)
                        type = ResponseType.valueOf(response.getType());
                }
                catch (IllegalArgumentException e) {
                    Log.e(TAG,"",e);
                }

                Observable<Observable<byte[]>> listenObservable;
                if(type==ResponseType.notification) {
                    listenObservable = bleConnection.setupNotification(UUID.fromString(response.getCharacteristic()));
                }
                else {
                    listenObservable = bleConnection.setupIndication(UUID.fromString(response.getCharacteristic()));
                }
                emitter.onNext(listenObservable);
            }
        }).flatMap(listenObservable ->
                listenObservable
                        .flatMap(responseBytesObservable -> responseBytesObservable)
                        .map(BytesUtils::byteArrayToHex));
    }

    @Override
    public Single<Device> scan(String deviceName) {
        return Single.create((SingleOnSubscribe<Device>)  emitter -> {

            dispose(scanDisposable);

            scanDisposable = rxBleClient.scanBleDevices()
                    .filter(rxBleScanResult -> {
                        Log.d(TAG,"Found BleDevice: "+rxBleScanResult.getBleDevice().getName());
                            return rxBleScanResult.getBleDevice().getName()!=null && rxBleScanResult.getBleDevice().getName().contains(deviceName);
                    })
                    .firstOrError()
                    .flatMapCompletable(rxBleScanResult -> {
                        dispose(scanDisposable);
                        Device device = new Device(rxBleScanResult.getBleDevice().getName(), rxBleScanResult.getBleDevice().getMacAddress());
                        bleDevices.put(device, rxBleScanResult.getBleDevice());
                        emitter.onSuccess(device);
                        return Completable.complete();
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> {
                        Log.d(TAG, "scan finished!");
                    }, throwable -> {
                        Log.e(TAG, "", throwable);
                        emitter.onError(throwable);
                    });

//            final ScanSettings settings = new ScanSettings.Builder()
//                    .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
//                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
//                    .build();
//
//            final ScanFilter filter = new ScanFilter.Builder()
//                    .setDeviceName(deviceName)
//                    .build();
//
//            rxBleClient.scanBleDevices(settings, filter)
//                    .firstOrError()
//                    .subscribeOn(Schedulers.io())
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribe(new SingleObserver<ScanResult>() {
//                        @Override
//                        public void onSubscribe(Disposable d) {
//                            dispose(scanDisposable);
//                            scanDisposable = d;
//                        }
//
//                        @Override
//                        public void onSuccess(ScanResult scanResult) {
//                            Device device = new Device(scanResult.getBleDevice().getName(), scanResult.getBleDevice().getMacAddress());
//                            bleDevices.put(device, scanResult.getBleDevice());
//                            dispose(scanDisposable);
//                            emitter.onSuccess(device);
//                        }
//
//                        @Override
//                        public void onError(Throwable e) {
//                            emitter.onError(e);
//                        }
//                    });
        }).doOnDispose(() -> {
            dispose(scanDisposable);
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Completable prepareBeforeSendingCommand(Request request) {
        return Completable.complete();
    }

    @Override
    public Single<Boolean> isConnected(final Device device) {
        return Single.create(emitter -> {
            if(device!=null) {
                RxBleDevice bleDevice = bleDevices.get(device);
                if(bleDevice==null)
                    bleDevice = rxBleClient.getBleDevice(device.getMacAddress());
                if(bleDevice==null)
                    emitter.onSuccess(Boolean.FALSE);
                else {
                    emitter.onSuccess(bleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED);
                }
            }
            else
                emitter.onSuccess(Boolean.FALSE);
        });
    }

    private void dispose(Disposable disposable) {
        if(disposable!=null && !disposable.isDisposed())
            disposable.dispose();
    }

    private void disposeDeviceConnections(Device device) {
        CompositeDisposable disposable = devicesDisposable.get(device);
        dispose(disposable);
        devicesDisposable.put(device, new CompositeDisposable());
    }

    private Completable createBond(BluetoothDevice device) {
        return Completable.create(emitter -> {
            mContext.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    BluetoothDevice extraDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                    switch (state) {
                        case BluetoothDevice.BOND_BONDED:
                            // Log.d(TAG,"Bond succeeded");
                            mContext.unregisterReceiver(this);
                            emitter.onComplete();
                            break;
                        case BluetoothDevice.BOND_NONE:
                            // Log.d(TAG,"Bond failed!");
                            mContext.unregisterReceiver(this);
                            emitter.onError(new Exception("Bond failed"));
                            break;
                    }
                }
            }, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
            device.createBond();
        });
    }

    private Observable<RxBleConnection> establishConnection(RxBleDevice rxDeviceBle) {
        return rxDeviceBle.establishConnection(false, new Timeout(30000, TimeUnit.MILLISECONDS))
//                .flatMap((Function<RxBleConnection, ObservableSource<RxBleConnection>>) rxBleConnection ->
//                        rxBleConnection.queue(new GattCacheRefresherOperation())
//                                .flatMap(refreshed -> Observable.just(rxBleConnection)))
                .subscribeOn(Schedulers.io());
    }

    private Single<Boolean> isBonded(String macAddress) {
        return Single.create(emitter -> {
            if(mBluetoothAdapter==null) {
                emitter.onError(new Exception("BluetoothAdapter is null, it's not permitted here!"));
            }
            else {
                Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
                if(bondedDevices!=null && !bondedDevices.isEmpty()) {
                    for (BluetoothDevice device : bondedDevices) {
                        if(device.getAddress().equals(macAddress)) {
                            emitter.onSuccess(Boolean.TRUE);
                            return;
                        }
                    }
                }
                emitter.onSuccess(Boolean.FALSE);
            }
        });
    }

    private Single<Boolean> checkNotAlreadyConnected(Device device) {
        return Single.create(emitter -> {
            RxBleConnection bleConnection = devicesConnection.get(device);
            RxBleDevice bleDevice = bleDevices.get(device);
            if(bleDevice!=null && bleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED) {
                if(bleConnection!=null)
                    emitter.onSuccess(Boolean.TRUE);
            }
            else if(bleConnection!=null) {
                devicesConnection.remove(device);
                disposeDeviceConnections(device);
            }
            emitter.onSuccess(Boolean.FALSE);
        });
    }

    private Completable connectInternal(Device device, boolean createBond) {
        return Completable.create(emitter -> {

            disposeDeviceConnections(device);

            RxBleDevice rxDeviceBle = bleDevices.get(device);
            if(rxDeviceBle==null) {
                rxDeviceBle = rxBleClient.getBleDevice(device.getMacAddress());
                bleDevices.put(device, rxDeviceBle);
            }

//            Disposable observerConnectionDisposable = rxDeviceBle.observeConnectionStateChanges()
//                    .subscribe(rxBleConnectionState -> {
//                        Log.d(TAG, "state="+rxBleConnectionState.name());
//                    }, throwable -> {
//                        Log.e(TAG, "", throwable);
//                    });

//            devicesDisposable.get(device).add(observerConnectionDisposable);

            RxBleDevice finalRxDeviceBle = rxDeviceBle;
                    isBonded(rxDeviceBle.getMacAddress())
                            .flatMapCompletable(bonded -> {
                                if(!createBond || bonded)
                                    return Completable.complete();
                                else
                                    return createBond(finalRxDeviceBle.getBluetoothDevice());
                            })
                            .andThen(establishConnection(rxDeviceBle))
                            .retry(3)
                            .doOnDispose(() -> {
                                Log.d(TAG,"Dispose of connectInternal::establishConnection");
                            })
                            .subscribe(new Observer<RxBleConnection>() {
                                @Override
                                public void onSubscribe(Disposable d) {
                                    devicesDisposable.get(device).add(d);
                                }

                                @Override
                                public void onNext(RxBleConnection rxBleConnection) {
                                    devicesConnection.put(device, rxBleConnection);
                                    if(!emitter.isDisposed())
                                        emitter.onComplete();
                                }

                                @Override
                                public void onError(Throwable e) {
                                    if(!emitter.isDisposed())
                                        emitter.onError(e);
                                    else
                                        Log.e(TAG,"",e);
                                    disposeDeviceConnections(device);
                                }

                                @Override
                                public void onComplete() {
                                }
                            });
        })
                .doOnError(throwable -> disposeDeviceConnections(device))
                .subscribeOn(Schedulers.io());
    }

    private Single<RxBleDeviceServices> getServices(Device device) {
//        if(rxBleConnection==null)
//            return Single.error(new IllegalArgumentException("Connection is null, cannot get services"));
//        return rxBleConnection.discoverServices(30000, TimeUnit.MILLISECONDS)
//                .subscribeOn(AndroidSchedulers.mainThread())
//                .observeOn(AndroidSchedulers.mainThread());
        return null;
    }

    @RestrictTo(RestrictTo.Scope.TESTS)
    public Disposable getScanDisposable() {
        return scanDisposable;
    }

    @RestrictTo(RestrictTo.Scope.TESTS)
    public Map<Device, CompositeDisposable> getDevicesDisposable() {
        return devicesDisposable;
    }

    @RestrictTo(RestrictTo.Scope.TESTS)
    public Map<Device, RxBleConnection> getDevicesConnection() {
        return devicesConnection;
    }

    @RestrictTo(RestrictTo.Scope.TESTS)
    public Map<Device, RxBleDevice> getBleDevices() {
        return bleDevices;
    }
}
