package com.telen.ble.manager.devices;

import com.telen.ble.manager.data.Device;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface GenericDevice {
    Single<Device> connect();
    Completable disconnect(Device device);
}
