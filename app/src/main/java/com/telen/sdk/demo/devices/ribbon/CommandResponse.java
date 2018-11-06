package com.telen.sdk.demo.devices.ribbon;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CommandResponse {

    @SerializedName("OK")
    @Expose
    private boolean ok;

    @SerializedName("err_code")
    @Expose
    private int errCode;

    @SerializedName("err_msg")
    @Expose
    private String errMsg;

    @SerializedName("Result")
    @Expose
    private String result;

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public int getErrCode() {
        return errCode;
    }

    public void setErrCode(int errCode) {
        this.errCode = errCode;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
