package com.ss.fat32;

import java.nio.ByteBuffer;

public class BufferUtils {

    public static final int readUint16(ByteBuffer buffer){
        byte b = buffer.get();
        byte b1 = buffer.get();
        return (b1 << 8) | b;
    }

    public static long readL32(ByteBuffer buffer){
        byte b = buffer.get();
        byte b1 = buffer.get();
        byte b2 = buffer.get();
        byte b3 = buffer.get();
        return b & 0xFF | (b1 & 0xFF) << 8 | (b2 & 0xFF) << 16 | (b3 & 0xFF) << 24;
    }


}
