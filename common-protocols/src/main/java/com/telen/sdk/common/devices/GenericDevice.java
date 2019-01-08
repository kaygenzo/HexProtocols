package com.telen.sdk.common.devices;

import com.telen.sdk.common.models.Device;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface GenericDevice {
    Single<Device> scan();
    Single<Device> connect(Device device, boolean bind);
    Completable disconnect(Device device);
    Single<Boolean> isConnected(Device device);
}
