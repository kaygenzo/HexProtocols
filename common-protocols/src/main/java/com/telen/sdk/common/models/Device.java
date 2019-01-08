package com.telen.sdk.common.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public class Device implements Parcelable {
    private String name;
    private String macAddress;

    public Device(String name, String macAddress) {
        this.name = name;
        this.macAddress = macAddress;
    }

    protected Device(Parcel in) {
        name = in.readString();
        macAddress = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(macAddress);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Device> CREATOR = new Creator<Device>() {
        @Override
        public Device createFromParcel(Parcel in) {
            return new Device(in);
        }

        @Override
        public Device[] newArray(int size) {
            return new Device[size];
        }
    };

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

    public String getName() {
        return name;
    }

    public String getMacAddress() {
        return macAddress;
    }

    @Override
    public String toString() {
        return "Device{" +
                "name='" + name + '\'' +
                ", macAddress='" + macAddress + '\'' +
                '}';
    }
}
