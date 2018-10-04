package com.telen.ble.manager.layers.impl;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.polidea.rxandroidble2.Timeout;
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.polidea.rxandroidble2.scan.ScanSettings;
import com.telen.ble.manager.model.Device;
import com.telen.ble.manager.layers.HardwareLayerInterface;
import com.telen.ble.manager.utils.BytesUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class BleHardwareConnectionLayer implements HardwareLayerInterface {

    private Context mContext;
    private RxBleClient rxBleClient;
    private BluetoothAdapter mBluetoothAdapter;

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
            else {
                if(!devicesDisposable.containsKey(device))
                    devicesDisposable.put(device, new CompositeDisposable());
                devicesDisposable.get(device).clear();

                RxBleDevice rxDeviceBle = bleDevices.get(device);
                if(rxDeviceBle==null) {
                    rxDeviceBle = rxBleClient.getBleDevice(device.getMacAddress());
                    bleDevices.put(device, rxDeviceBle);
                }

                Disposable observerConnectionDisposable = rxDeviceBle.observeConnectionStateChanges()
                        .subscribe(rxBleConnectionState -> {
                            Log.d(TAG, "state="+rxBleConnectionState.name());
                        });

                devicesDisposable.get(device).add(observerConnectionDisposable);

                DisposableObserver<RxBleConnection> disposableObserver = new DisposableObserver<RxBleConnection>() {
                    @Override
                    public void onNext(RxBleConnection rxBleConnection) {
                        devicesConnection.put(device, rxBleConnection);
                        emitter.onComplete();
                    }

                    @Override
                    public void onError(Throwable e) {
                        if(!emitter.isDisposed())
                            emitter.onError(e);
                        else
                            Log.e(TAG,"",e);
                    }

                    @Override
                    public void onComplete() {
                    }
                };

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
                        .subscribe(disposableObserver);

                devicesDisposable.get(device).add(disposableObserver);
            }
        });
    }

    @Override
    public Completable disconnect(Device device) {
        return Completable.create(emitter -> {
            if(devicesDisposable.containsKey(device))
                devicesDisposable.get(device).clear();
            RxBleConnection bleConnection =  devicesConnection.remove(device);
            emitter.onComplete();
        });
    }

    @Override
    public Single<String> sendCommand(Device device, UUID characteristic, String command) {
        return sendCommand(device, characteristic, BytesUtils.hexStringToByteArray(command))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .doOnError(throwable -> Log.e(TAG,"sendCommand",throwable))
                .doOnSuccess(response -> Log.d(TAG,"sendCommand: response="+response));
    }

    @Override
    public Single<String> sendCommand(Device device, UUID characteristic, byte[] command) {
        return Single.create(emitter -> {
            RxBleConnection bleConnection = devicesConnection.get(device);
            if(bleConnection == null) {
                emitter.onError(new Exception("No existing connection to this device"));
            }
            else {
                bleConnection.writeCharacteristic(characteristic, command)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .doOnError(throwable -> Log.e(TAG,"sendCommand",throwable))
                        .doOnSuccess(response -> Log.d(TAG,"sendCommand: response="+response))
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
        });
    }

    @Override
    public Observable<String> listenResponses(Device device, UUID uuid) {
        return Observable.create(emitter -> {
            RxBleConnection bleConnection = devicesConnection.get(device);
            if(bleConnection == null) {
                emitter.onError(new Exception("Connection is null, cannot listen for response frames"));
            }
            else {
                bleConnection.setupNotification(uuid)
                        .flatMap(responseBytesObservable -> responseBytesObservable)
                        .map(BytesUtils::byteArrayToHex)
                        .subscribe(new Observer<String>() {
                            @Override
                            public void onSubscribe(Disposable d) {
                                devicesDisposable.get(device).add(d);
                            }

                            @Override
                            public void onNext(String s) {
                                emitter.onNext(s);
                            }

                            @Override
                            public void onError(Throwable e) {
                                emitter.onError(e);
                            }

                            @Override
                            public void onComplete() {
                                emitter.onComplete();
                            }
                        });
            }
        });
    }

    @Override
    public Single<Device> scan(String deviceName) {
        return Single.create(emitter -> {

            final ScanSettings settings = new ScanSettings.Builder()
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                    .build();

            final ScanFilter filter = new ScanFilter.Builder()
                    .setDeviceName(deviceName)
                    .build();

            rxBleClient.scanBleDevices(settings, filter)
                    .firstOrError()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<ScanResult>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            dispose(scanDisposable);
                            scanDisposable = d;
                        }

                        @Override
                        public void onSuccess(ScanResult scanResult) {
                            Device device = new Device(scanResult.getBleDevice().getName(), scanResult.getBleDevice().getMacAddress());
                            bleDevices.put(device, scanResult.getBleDevice());
                            dispose(scanDisposable);
                            emitter.onSuccess(device);
                        }

                        @Override
                        public void onError(Throwable e) {
                            emitter.onError(e);
                        }
                    });
        });
    }

    @Override
    public Single<Device> scanOld(String deviceName) {
        return Single.create(emitter -> {

            dispose(scanDisposable);

            scanDisposable = rxBleClient.scanBleDevices()
                    .filter(rxBleScanResult ->
                            rxBleScanResult.getBleDevice().getName()!=null && rxBleScanResult.getBleDevice().getName().equals(deviceName))
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
                    .subscribe();
        });
    }

    @Override
    public Single<RxBleDeviceServices> getServices(Device device) {
//        if(rxBleConnection==null)
//            return Single.error(new IllegalArgumentException("Connection is null, cannot get services"));
//        return rxBleConnection.discoverServices(30000, TimeUnit.MILLISECONDS)
//                .subscribeOn(AndroidSchedulers.mainThread())
//                .observeOn(AndroidSchedulers.mainThread());
        return null;
    }

    @Override
    public Single<Boolean> isBonded(String macAddress) {
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

    private void dispose(Disposable disposable) {
        if(disposable!=null && !disposable.isDisposed())
            disposable.dispose();
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
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Map<Device, CompositeDisposable> getDevicesDisposable() {
        return devicesDisposable;
    }

    public Map<Device, RxBleConnection> getDevicesConnection() {
        return devicesConnection;
    }

    public Map<Device, RxBleDevice> getBleDevices() {
        return bleDevices;
    }
}
