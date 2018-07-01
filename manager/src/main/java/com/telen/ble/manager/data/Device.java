package com.telen.ble.manager.data;

import java.util.Objects;

public class Device {
    private String name;
    private String macAddress;

    public Device(String name, String macAddress) {
        this.name = name;
        this.macAddress = macAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Device device = (Device) o;
        return Objects.equals(name, device.name) &&
                Objects.equals(macAddress, device.macAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, macAddress);
    }
}
