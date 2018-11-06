package com.telen.ble.sdk.layers;

import com.telen.ble.sdk.model.Device;
import com.telen.ble.sdk.model.Request;
import com.telen.ble.sdk.model.Response;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;

public interface HardwareLayerInterface {
    Completable connect(Device device, boolean createBond);
    Completable disconnect(Device device);
    Single<String> sendCommand(Device device, Request request, String command);
    Single<String> sendCommand(Device device, Request request, byte[] command);
    Observable<String> listenResponses(Device device, Response response);
    Single<Device> scan(String deviceName);
    Single<Device> scanOld(String deviceName);
    Single<Boolean> isBonded(String macAddress);
    Completable preProcessBeforeSendingCommand(Request request);
}