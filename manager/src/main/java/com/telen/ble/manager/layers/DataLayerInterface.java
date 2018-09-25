package com.telen.ble.manager.layers;

import com.telen.ble.manager.model.Command;
import com.telen.ble.manager.model.Device;

import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;

public interface DataLayerInterface {
    Single<Device> scan(String deviceName);
    Single<Device> connect(Device device, boolean createBond);
    Completable disconnect(Device device);
    Observable<String> sendCommand(Device device, Command command, Map<String, Object> data);
    Single<Boolean> isBonded(Device device);
}
