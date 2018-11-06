package com.telen.sdk.common.utils;

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

    public static String toString(byte[] arrayOfByte) {
        StringBuilder builder = new StringBuilder();
        if ((arrayOfByte != null) && (arrayOfByte.length > 0)) {
            for (int i = 0; i < arrayOfByte.length; i++)
            {
                String str = Integer.toHexString(arrayOfByte[i] & 0xFF);
                if (str.length() < 2) {
                    builder.append(0);
                }
                builder.append(str);
                builder.append(" ");
            }
            return builder.toString().trim();
        }
        return null;
    }

    public static byte getSumOfBytes(byte[] array, int length) {
        int i = 0;
        if (array.length < length) {
            return 0;
        }
        int j = 0;
        while (i < length)
        {
            j += (array[i] & 0xFF);
            i++;
        }
        return (byte)j;
    }
}
