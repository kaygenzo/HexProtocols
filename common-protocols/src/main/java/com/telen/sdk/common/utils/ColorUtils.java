package com.telen.sdk.common.utils;

public class ColorUtils {
    public static int[] getRGB(int color) {
        int r = (color & 0xFF0000) >> 16;
        int g = (color & 0xFF00) >> 8;
        int b = (color & 0xFF);
        return new int[] {r,g,b};
    }
}
