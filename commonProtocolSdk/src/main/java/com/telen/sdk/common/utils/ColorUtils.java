package com.telen.sdk.common.utils;

public class ColorUtils {
    public static int[] getRGB(int hexColor) {
        int r = (hexColor & 0xFF0000) >> 16;
        int g = (hexColor & 0xFF00) >> 8;
        int b = (hexColor & 0xFF);
        return new int[] {r,g,b};
    }
}
