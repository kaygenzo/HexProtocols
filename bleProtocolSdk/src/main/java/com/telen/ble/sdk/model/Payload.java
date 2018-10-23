package com.telen.ble.sdk.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Payload {
    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("start")
    @Expose
    private int start;

    @SerializedName("end")
    @Expose
    private int end;

    @SerializedName("min")
    @Expose
    private String min;

    @SerializedName("max")
    @Expose
    private String max;

    @SerializedName("type")
    @Expose
    private String type;

    @SerializedName("value")
    @Expose
    private String value;

    @SerializedName("direction")
    @Expose
    private String direction;

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getMin() {
        return min;
    }

    public void setMin(String min) {
        this.min = min;
    }

    public void setMin(int min) {
        this.min = String.valueOf(min);
    }

    public String getMax() {
        return max;
    }

    public void setMax(String max) {
        this.max = max;
    }

    public void setMax(int max) {
        this.max = String.valueOf(max);
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }
}
