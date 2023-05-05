package com.ss.fat32;

public class Utils {

    public static final long toInt(byte[] bytes) {
        return (bytes[0] & 0xFF) | ((bytes[1] & 0xFF) << 8) | ((bytes[2] & 0xFF) << 16) | ((bytes[3] & 0xFF) << 24);
    }

    public static final int getUint16(byte[] src,int offset){
        final int v0 = src[offset + 0] & 0xFF;
        final int v1 = src[offset + 1] & 0xFF;
        return ((v1 << 8) | v0);
    }


}
