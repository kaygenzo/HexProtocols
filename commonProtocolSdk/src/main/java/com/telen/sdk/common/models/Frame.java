package com.telen.sdk.common.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Frame {

    @SerializedName("payloads")
    @Expose
    private List<Payload> payloads;

    @SerializedName("commandIndex")
    @Expose
    private int commandIndex = -1;

    @SerializedName("commandId")
    @Expose
    private int commandId = -1;

    public List<Payload> getPayloads() {
        return payloads;
    }

    public void setPayloads(List<Payload> payloads) {
        this.payloads = payloads;
    }

    public int getCommandIndex() {
        return commandIndex;
    }

    public int getCommandId() {
        return commandId;
    }
}
