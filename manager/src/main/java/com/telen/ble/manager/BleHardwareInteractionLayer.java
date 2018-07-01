package com.telen.ble.manager;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.polidea.rxandroidble2.RxBleScanResult;
import com.polidea.rxandroidble2.Timeout;
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.polidea.rxandroidble2.scan.ScanSettings;
import com.telen.ble.manager.interfaces.HardwareLayerInterface;
import com.telen.ble.manager.utils.BytesUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.MaybeObserver;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.schedulers.Schedulers;

public class BleHardwareInteractionLayer implements HardwareLayerInterface {

    private RxBleClient rxBleClient;
    private Disposable scanDisposable;
    private static final String TAG = BleHardwareInteractionLayer.class.getSimpleName();

    public BleHardwareInteractionLayer(RxBleClient rxBleClient) {
        this.rxBleClient = rxBleClient;
    }

    @Override
    public Single<RxBleConnection> connect(final RxBleDevice device) {
        return Single.create(emitter -> {
            if(device==null)
                emitter.onError(new IllegalArgumentException("Cannot connect to a null device"));
            else
                device.establishConnection(false, new Timeout(30000, TimeUnit.MILLISECONDS))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                rxBleConnection ->emitter.onSuccess(rxBleConnection),
                                throwable -> emitter.onError(throwable),
                                () -> {},
                                disposable -> {}
                        );
        });
//                .doOnNext(rxBleConnection -> {
//                    Log.d(TAG,"rxBleConnection="+rxBleConnection);
//                    rxBleConnection.discoverServices(30000, TimeUnit.MILLISECONDS)
//                            .subscribe(new SingleObserver<RxBleDeviceServices>() {
//                                @Override
//                                public void onSubscribe(Disposable d) {}
//
//                                @Override
//                                public void onSuccess(RxBleDeviceServices rxBleDeviceServices) {
//                                    final StringBuilder builder= new StringBuilder();
//                                    for (BluetoothGattService gattService : rxBleDeviceServices.getBluetoothGattServices()) {
//                                        builder.append("Service: UUID: "+gattService.getUuid()+"\n");
//                                        for (BluetoothGattCharacteristic serviceCharacteristic : gattService.getCharacteristics()) {
//                                            builder.append("\tcharacteristic: UUID: "+serviceCharacteristic.getUuid()+"\n");
//                                            for(BluetoothGattDescriptor descriptor : serviceCharacteristic.getDescriptors()) {
//                                                builder.append("\t\tdescriptor: UUID: "+descriptor.getUuid()+"\n");
//                                            }
//                                        }
//                                    }
//                                    Log.d(TAG,builder.toString());
//                                }
//
//                                @Override
//                                public void onError(Throwable e) {
//                                    Log.e(TAG,"",e);
//                                }
//                            });
//                })
    }

    @Override
    public Single<String> sendCommand(RxBleConnection rxBleConnection, UUID characteristic, String command) {
        return sendCommand(rxBleConnection, characteristic, BytesUtils.hexStringToByteArray(command))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(throwable -> Log.e(TAG,"sendCommand",throwable))
                .doOnSuccess(response -> Log.d(TAG,"sendCommand: response="+response));
    }

    @Override
    public Single<String> sendCommand(RxBleConnection rxBleConnection, UUID characteristic, byte[] command) {
        return rxBleConnection.writeCharacteristic(characteristic, command).map(BytesUtils::byteArrayToHex);
    }

    @Override
    public Observable<String> listenResponses(RxBleConnection rxBleConnection, UUID uuid) {
        if(rxBleConnection==null)
            return Observable.error(new IllegalArgumentException("Connection is null, cannot listen for response frames"));
        return rxBleConnection.setupIndication(uuid)
                .flatMap(responseBytesObservable -> responseBytesObservable)
                .map(BytesUtils::byteArrayToHex);
    }

    @Override
    public Observable<ScanResult> scan(String deviceName) {
        ScanSettings settings = new ScanSettings.Builder()
                .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();

        ScanFilter filter = new ScanFilter.Builder()
                .setDeviceName(deviceName)
                .build();

        return rxBleClient.scanBleDevices(settings, filter)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<RxBleScanResult> scanOld(String deviceName) {
        return Observable.create(emitter -> {

            if(scanDisposable!=null && !scanDisposable.isDisposed())
                scanDisposable.dispose();

            scanDisposable = rxBleClient.scanBleDevices()
                    .filter(rxBleScanResult ->
                            rxBleScanResult.getBleDevice().getName()!=null && rxBleScanResult.getBleDevice().getName().equals(deviceName))
                    .firstElement()
                    .flatMapCompletable(rxBleScanResult -> {
                        if(scanDisposable!=null && !scanDisposable.isDisposed())
                            scanDisposable.dispose();
                        emitter.onNext(rxBleScanResult);
                        emitter.onComplete();
                        return Completable.complete();
                    })
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe();
        });
    }

    @Override
    public Single<RxBleDeviceServices> getServices(RxBleConnection rxBleConnection) {
        if(rxBleConnection==null)
            return Single.error(new IllegalArgumentException("Connection is null, cannot get services"));
        return rxBleConnection.discoverServices(30000, TimeUnit.MILLISECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
