package com.telen.sdk.demo.commonui;

import com.telen.sdk.common.models.DeviceType;

public enum DeviceInfo {
    MINGER(DeviceType.LIGHTBULB, "minger.json"),
    RIBBON(DeviceType.RIBBON, "led_ribbon.json");

    private final DeviceType type;
    private final String assetPath;

    DeviceInfo(DeviceType type, String assetPath) {
        this.type = type;
        this.assetPath = assetPath;
    }

    public DeviceType getType() {
        return type;
    }

    public String getProtocolPath() {
        return assetPath;
    }

}
