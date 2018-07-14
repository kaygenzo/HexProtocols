package com.telen.ble.manager.data;

public enum DeviceInfo {
    MINGER(DeviceType.LIGHTBULB, "minger.json"),
    SIMULATOR(DeviceType.SIMULATOR,"test.json");

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
