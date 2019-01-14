package com.telen.sdk.demo.commonui.devices.ribbon;

import com.telen.sdk.common.utils.BytesUtils;

import java.util.Map;

public abstract class DevicesCMD {

    private String hexData;
    private String macAddress;

    public String getHexData() {
        return hexData;
    }

    public void setHexData(String hexData) {
        this.hexData = hexData;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

//    interface Factory<T> {
//        T factory();
//    }
//
//    public class Builder {
//        public static class LightOnFactory implements Factory
//    }

    public static class Builder<T extends DevicesCMD> {
        private DevicesCMD instance;
        private Map<String, Integer> mData;

        public Builder(Class<T> instanceClass) {
            try {
                instance = instanceClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        public Builder setMacAddress(String macAddress) {
            this.instance.setMacAddress(macAddress);
            return this;
        }

        public Builder withData(Map<String, Integer> data) {
            mData = data;
            return this;
        }

        public DevicesCMD build() throws IllegalArgumentException {
            byte[] hexDataArray = instance.getHexDataArray(mData);
            if(hexDataArray == null)
                throw new IllegalArgumentException("Data cannot be null");
            String hexData = BytesUtils.byteArrayToHex(hexDataArray);
            instance.setHexData(hexData);
            return instance;
        }
    }

    public abstract byte[] getHexDataArray(Map<String, Integer> data);

    @Override
    public String toString() {
        return "DevicesCMD{" +
                "hexData='" + hexData + '\'' +
                ", macAddress='" + macAddress + '\'' +
                '}';
    }
}
