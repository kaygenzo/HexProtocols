package com.telen.ble.blemanagersample;

import com.telen.ble.manager.model.DeviceType;

public enum DeviceInfo {
    MINGER(DeviceType.LIGHTBULB, "minger.json");

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
