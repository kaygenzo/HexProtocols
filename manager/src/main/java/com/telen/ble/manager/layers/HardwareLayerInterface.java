package com.telen.ble.manager.layers;

import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.telen.ble.manager.model.Device;

import java.util.UUID;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;

public interface HardwareLayerInterface {
    Completable connect(Device device);
    Completable disconnect(Device device);
    Single<String> sendCommand(Device device, UUID characteristic, String command);
    Single<String> sendCommand(Device device, UUID characteristic, byte[] command);
    Observable<String> listenResponses(Device device, UUID uuid);
    Single<Device> scan(String deviceName);
    Single<Device> scanOld(String deviceName);
    Single<RxBleDeviceServices> getServices(Device device);
}