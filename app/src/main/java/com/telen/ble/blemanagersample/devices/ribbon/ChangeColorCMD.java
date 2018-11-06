package com.telen.ble.blemanagersample.devices.ribbon;

import com.telen.sdk.common.utils.BytesUtils;

import java.util.Map;

public class ChangeColorCMD extends DevicesCMD {

    @Override
    public byte[] getHexDataArray(Map<String, Integer> data) {

        byte[] bytes = new byte[8];
        bytes[0] = (byte)0x31; // subroutine
        bytes[1] = data.get("RED").byteValue();
        bytes[2] = data.get("GREEN").byteValue();
        bytes[3] = data.get("BLUE").byteValue();
        bytes[4] = (byte)0;
        bytes[5] = (byte)0;
        bytes[6] = (byte)0x0F;
        bytes[7] = BytesUtils.getSumOfBytes(bytes, bytes.length-1); //control

        return bytes;
    }
}
