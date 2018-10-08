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

    public static String[] splitStringByLength(String string, int length) {
        if(string!=null && string.length()>0) {
            String[] result = new String[(string.length()+1)/length];
            int index = 0;
            int cpt = 0;
            while (index<string.length()) {
                String subString = string.substring(index, Math.min(index+length, string.length()));
                result[cpt] = subString;
                cpt++;
                index = index + length;
            }
            return result;
        }
        return null;
    }

    public static String reverseBytes(String hexBytes) {
        String[] split = BytesUtils.splitStringByLength(hexBytes, 2);
        StringBuilder reversed = new StringBuilder();
        for (int i = split.length-1; i>=0; i--) {
            reversed.append(split[i]);
        }
        return reversed.toString();
    }
}
