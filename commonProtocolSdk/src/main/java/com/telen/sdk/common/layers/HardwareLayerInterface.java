package com.telen.sdk.common.layers;

import com.telen.sdk.common.models.Device;
import com.telen.sdk.common.models.Request;
import com.telen.sdk.common.models.Response;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;

public interface HardwareLayerInterface {
    Completable connect(Device device, boolean bind);
    Completable disconnect(Device device);
    Single<String> sendCommand(Device device, Request request, String command);
    Single<String> sendCommand(Device device, Request request, byte[] command);
    Observable<String> listenResponses(Device device, Response response);
    Single<Device> scan(String deviceName);
    Completable prepareBeforeSendingCommand(Request request);
}