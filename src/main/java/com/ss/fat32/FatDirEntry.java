package com.ss.fat32;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class FatDirEntry {

    public String fileName;

    private boolean isLongNameEntry;

    public LongNameEntry longNameEntry;

    public FatDirEntry(ByteBuffer b){
        b.order(ByteOrder.LITTLE_ENDIAN);
        byte attr = b.get(11);
        //long name
        if ((attr == Fat32Reader.ATTR_LONG_NAME)){
            this.isLongNameEntry = true;
        }
        if (isLongNameEntry){
            this.longNameEntry = new LongNameEntry();
            longNameEntry.ldirOrd = (b.get() & 0xff);
            assert longNameEntry.ldirOrd > 0;
            b.getChar(5);
            boolean end = readToChars(longNameEntry.ldirName1, b, 5);
            longNameEntry.ldirAttr = b.get();
            longNameEntry.ldirType = b.get();
            assert longNameEntry.ldirType == 0;
            longNameEntry.ldirChksum = b.get();
            if (end) {
                b.position(b.position() + 6 * 2);
            } else {
                end = readToChars(longNameEntry.ldirName2, b, 6);
            }
            readToBytes(longNameEntry.ldirFstClusLO, b, 2);
            assert longNameEntry.ldirFstClusLO[0] == 0;
            assert longNameEntry.ldirFstClusLO[1] == 0;
            if (end) {
                b.position(b.position() + 2 * 2);
            } else {
                readToChars(longNameEntry.ldirName3, b, 2);
            }
        }
    }
    private void readToBytes(byte[] dst, ByteBuffer src, int length) {
        for (int i = 0; i < length; i++) {
            dst[i] = src.get();
        }
    }
    public boolean readToChars(char[] chars,ByteBuffer buffer,int len){
        for (int i = 0; i < len; i++) {
            chars[i] = buffer.getChar();
        }
        return false;
    }

    public boolean isLongNameEntry(){
        return isLongNameEntry;
    }





}