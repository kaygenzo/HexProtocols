package com.telen.ble.manager;

import android.util.Log;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.polidea.rxandroidble2.Timeout;
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.polidea.rxandroidble2.scan.ScanSettings;
import com.telen.ble.manager.data.Device;
import com.telen.ble.manager.interfaces.HardwareLayerInterface;
import com.telen.ble.manager.utils.BytesUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class BleHardwareInteractionLayer implements HardwareLayerInterface {

    private RxBleClient rxBleClient;

    private Map<Device, RxBleDevice> bleDevices = new HashMap<>();
    private Map<Device, CompositeDisposable> devicesDisposable = new HashMap<>();
    private Map<Device, RxBleConnection> devicesConnection = new HashMap<>();

    private Disposable scanDisposable;
    private static final String TAG = BleHardwareInteractionLayer.class.getSimpleName();

    public BleHardwareInteractionLayer(RxBleClient rxBleClient) {
        this.rxBleClient = rxBleClient;
    }

    @Override
    public Completable connect(Device device) {
        return Completable.create(emitter -> {
            if(device==null || !bleDevices.containsKey(device))
                emitter.onError(new IllegalArgumentException("Cannot connect to a null device"));
            else {
                if(!devicesDisposable.containsKey(device))
                    devicesDisposable.put(device, new CompositeDisposable());
                devicesDisposable.get(device).clear();

                RxBleDevice rxDeviceBle = bleDevices.get(device);

                Disposable observerConnectionDisposable = rxDeviceBle.observeConnectionStateChanges()
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe(rxBleConnectionState -> Log.d(TAG, "state="+rxBleConnectionState.name()));

                Disposable connectionEstablishmentDisposable = rxDeviceBle.establishConnection(false, new Timeout(30000, TimeUnit.MILLISECONDS))
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe(rxBleConnection -> {
                            devicesConnection.put(device, rxBleConnection);
                            emitter.onComplete();
                        }, emitter::onError, () ->{
                        });

                devicesDisposable.get(device).add(observerConnectionDisposable);
                devicesDisposable.get(device).add(connectionEstablishmentDisposable);
            }
        });
    }

    @Override
    public Completable disconnect(Device device) {
        return Completable.create(emitter -> {
            if(devicesDisposable.containsKey(device))
                devicesDisposable.get(device).clear();
            devicesConnection.get(device);
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
//        if(rxBleConnection==null)
//            return Observable.error(new IllegalArgumentException("Connection is null, cannot listen for response frames"));
//        return rxBleConnection.setupIndication(uuid)
//                .flatMap(responseBytesObservable -> responseBytesObservable)
//                .map(BytesUtils::byteArrayToHex);
        return Observable.empty();
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
                    .observeOn(Schedulers.io())
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
                    .observeOn(Schedulers.io())
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

    private void dispose(Disposable disposable) {
        if(disposable!=null && !disposable.isDisposed())
            disposable.dispose();
    }
}
