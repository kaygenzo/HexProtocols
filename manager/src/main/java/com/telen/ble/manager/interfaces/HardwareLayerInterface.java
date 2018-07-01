package com.telen.ble.manager.interfaces;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.polidea.rxandroidble2.RxBleScanResult;
import com.polidea.rxandroidble2.scan.ScanResult;

import java.util.UUID;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;

public interface HardwareLayerInterface {
    Single<RxBleConnection> connect(RxBleDevice device);
    Single<String> sendCommand(RxBleConnection rxBleConnection, UUID characteristic, String command);
    Single<String> sendCommand(RxBleConnection rxBleConnection, UUID characteristic, byte[] command);
    Observable<String> listenResponses(RxBleConnection rxBleConnection, UUID uuid);
    Observable<ScanResult> scan(String deviceName);
    Observable<RxBleScanResult> scanOld(String deviceName);
    Single<RxBleDeviceServices> getServices(RxBleConnection rxBleConnection);
}