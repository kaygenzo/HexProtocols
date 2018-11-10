package com.telen.sdk.socket.devices;

import com.telen.sdk.common.devices.GenericDevice;
import com.telen.sdk.common.models.Device;
import com.telen.sdk.common.models.RequestType;

import io.reactivex.Single;

public interface GenericNetworkDevice extends GenericDevice {
    Single<Device> configureNetwork(SocketDevice device, String ssid, String password);
    Single<Device> connect(SocketDevice device, RequestType requestType);
}
