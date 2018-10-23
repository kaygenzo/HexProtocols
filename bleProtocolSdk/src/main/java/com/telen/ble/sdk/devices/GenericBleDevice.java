package com.telen.ble.sdk.devices;

import com.telen.ble.sdk.model.Device;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface GenericBleDevice {
    Single<Device> scan();
    Single<Device> connect(Device device, boolean createBond);
    Completable disconnect(Device device);
}
