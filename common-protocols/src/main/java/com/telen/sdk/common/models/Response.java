package com.telen.sdk.common.models;

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

    @SerializedName("frames")
    @Expose
    private List<Frame> frames;

    @SerializedName("complete_on_timeout")
    @Expose
    private boolean completeOnTimeout;

    @SerializedName("timeout")
    @Expose
    private long timeout;

    @SerializedName("type")
    @Expose
    private String type;

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

    public String getEndFrame() {
        return endFrame;
    }

    public void setEndFrame(String endFrame) {
        this.endFrame = endFrame;
    }

    public boolean isCompleteOnTimeout() {
        return completeOnTimeout;
    }

    public void setCompleteOnTimeout(boolean completeOnTimeout) {
        this.completeOnTimeout = completeOnTimeout;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public String getType() {
        return type;
    }

    public List<Frame> getFrames() {
        return frames;
    }

    public void setFrames(List<Frame> frames) {
        this.frames = frames;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Frame getFrameByCommandId(int commandId) {
        if(frames!=null) {
            for (Frame frame: frames) {
                if(frame.getCommandId() == commandId)
                    return frame;
            }
        }
        return null;
    }
}
