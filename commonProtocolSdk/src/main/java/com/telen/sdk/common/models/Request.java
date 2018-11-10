package com.telen.sdk.common.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Request {
    @SerializedName("service")
    @Expose
    private String service;

    @SerializedName("characteristic")
    @Expose
    private String characteristic;

    @SerializedName("payloads")
    @Expose
    private List<Payload> payloads;

    @SerializedName("length")
    @Expose
    private int length;

    @SerializedName("timeout")
    @Expose
    private long timeout;

    @SerializedName("address")
    @Expose
    private String address;

    @SerializedName("port")
    @Expose
    private int port;

    @SerializedName("isBroadcast")
    @Expose
    private boolean isBroadcast;

    @SerializedName("type")
    @Expose
    private String type;

    public String getService() {
        return service;
    }

    public String getCharacteristic() {
        return characteristic;
    }

    public void setCharacteristic(String characteristic) {
        this.characteristic = characteristic;
    }

    public List<Payload> getPayloads() {
        return payloads;
    }

    public void setPayloads(List<Payload> payloads) {
        this.payloads = payloads;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isBroadcast() {
        return isBroadcast;
    }

    public String getType() {
        return type;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
