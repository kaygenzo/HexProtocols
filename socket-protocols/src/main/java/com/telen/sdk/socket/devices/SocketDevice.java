package com.telen.sdk.socket.devices;

import com.telen.sdk.common.models.Device;
import com.telen.sdk.socket.models.RequestType;

public class SocketDevice extends Device {
    private String address;
    private int port;
    private RequestType type;

    private SocketDevice(String name, String address, int port, RequestType type) {
        super(name, null);
        this.address = address;
        this.port = port;
        this.type = type;
    }

    public static class Builder {
        private String address;
        private int port;
        private RequestType type;
        private String name;

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withAddress(String address) {
            this.address = address;
            return this;
        }

        public Builder withPort(int port) {
            this.port = port;
            return this;
        }

        public Builder withType(RequestType type) {
            this.type = type;
            return this;
        }

        public SocketDevice build() {
            return new SocketDevice(name, address, port, type);
        }
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
