package com.telen.sdk.common.layers;

import com.telen.sdk.common.models.Command;
import com.telen.sdk.common.models.Device;

import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;

public interface DataLayerInterface<E> {
    Single<Device> scan(String deviceName);
    Single<Device> connect(Device device, boolean bind);
    Completable disconnect(Device device);
    Observable<String> sendCommand(Device device, Command command, Map<String, Object> data);
    Observable<String> sendCommand(Device device, Command command);
    Single<Boolean> isConnected(Device device);
}
