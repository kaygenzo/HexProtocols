package com.telen.ble.manager.devices;

import com.telen.ble.manager.model.Device;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface GenericDevice {
    Single<Device> scan();
    Single<Device> connect(Device device, boolean createBond);
    Completable disconnect(Device device);
}
