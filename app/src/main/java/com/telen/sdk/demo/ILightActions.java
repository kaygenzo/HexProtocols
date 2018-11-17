package com.telen.sdk.demo;

import com.telen.sdk.common.models.DeviceType;

public interface ILightActions {
    void lightOn();
    void lightOff();
    void setColor(int color);
}
