package com.telen.ble.sdk.model;

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
}
