package com.telen.sdk.socket.devices;

import com.telen.sdk.common.models.Device;
import com.telen.sdk.socket.models.RequestType;

public class SocketDevice extends Device {
    private String address;
    private int port;
    private RequestType type;

    public SocketDevice(String name) {
        super(name, null);
    }

    @Override
    public String toString() {
        return "SocketDevice{" +
                "name='" + getName() + '\'' +
                ", address='" + address + '\'' +
                ", port=" + port +
                ", type='" + type + '\'' +
                '}';
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public String getAddress() {
        return address;
    }

    public RequestType getType() {
        return type;
    }

    public void setType(RequestType type) {
        this.type = type;
    }
}
