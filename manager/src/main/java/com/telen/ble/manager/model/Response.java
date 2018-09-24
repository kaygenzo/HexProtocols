package com.telen.ble.manager.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Response {
    @SerializedName("service")
    @Expose
    private String service;

    @SerializedName("characteristic")
    @Expose
    private String characteristic;

    @SerializedName("end_frame")
    @Expose
    private String endFrame;

    @SerializedName("payloads")
    @Expose
    private List<Payload> payloads;

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
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

    public String getEndFrame() {
        return endFrame;
    }
}
