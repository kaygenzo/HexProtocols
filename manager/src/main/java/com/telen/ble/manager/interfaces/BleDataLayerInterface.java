package com.telen.ble.manager.interfaces;

import com.telen.ble.manager.data.Command;
import com.telen.ble.manager.data.Device;

import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;

public interface BleDataLayerInterface {
    Single<Device> connect(String deviceName);
    Completable disconnect(Device device);
    Observable<String> sendCommand(Device device, Command command, Map<String, Object> data);
}
