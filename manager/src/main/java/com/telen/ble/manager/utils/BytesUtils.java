package com.telen.ble.manager.utils;

public class BytesUtils {

    public static String byteArrayToHex(byte[] array) {
        StringBuilder sb = new StringBuilder(array.length * 2);
        for (byte b: array)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public static byte[] hexStringToByteArray(String hexaString) {
        int len = hexaString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte)((Character.digit(hexaString.charAt(i), 16) << 4)
                    + Character.digit(hexaString.charAt(i + 1), 16));
        }
        return data;
    }
}
