package com.telen.ble.manager.model;

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
}
