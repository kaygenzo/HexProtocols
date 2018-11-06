package com.telen.sdk.demo.devices.ribbon;

import com.telen.sdk.common.utils.BytesUtils;

import java.util.Map;

public class LightOnCMD extends DevicesCMD {

    @Override
    public byte[] getHexDataArray(Map<String, Integer> data) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte)0x71; // subroutine
        bytes[1] = (byte)0x23; //light on byte
        bytes[2] = (byte)0x0F;
        bytes[3] = BytesUtils.getSumOfBytes(bytes, bytes.length-1); //control
        return bytes;
    }
}
