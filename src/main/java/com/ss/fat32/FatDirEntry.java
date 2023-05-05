package com.ss.fat32;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class FatDirEntry {

    public String fileName;

    private boolean isLongNameEntry;

    public LongNameEntry longNameEntry;

    public Fat32System.Dir dir;

    public FatDirEntry(ByteBuffer b){
        b.order(ByteOrder.LITTLE_ENDIAN);
        byte attr = b.get(11);
        //long name
        if ((attr == Fat32System.ATTR_LONG_NAME)){
            this.isLongNameEntry = true;
        }
        if (isLongNameEntry){
            this.longNameEntry = new LongNameEntry();
            longNameEntry.ldirOrd = (b.get() & 0xff);
            assert longNameEntry.ldirOrd > 0;
            b.getChar(5);
            readChars(longNameEntry.ldirName1, b, 5);
            longNameEntry.ldirAttr = b.get();
            longNameEntry.ldirType = b.get();
            assert longNameEntry.ldirType == 0;
            longNameEntry.ldirChksum = b.get();
            readChars(longNameEntry.ldirName2, b, 6);
            readToBytes(longNameEntry.ldirFstClusLO, b, 2);
            assert longNameEntry.ldirFstClusLO[0] == 0;
            assert longNameEntry.ldirFstClusLO[1] == 0;
            readChars(longNameEntry.ldirName3, b, 2);
        }else{
            dir = new Fat32System.Dir();

        }
    }
    private void readToBytes(byte[] dst, ByteBuffer src, int length) {
        for (int i = 0; i < length; i++) {
            dst[i] = src.get();
        }
    }
    private void readChars(char[] dst, ByteBuffer src, int length) {
        for (int i = 0; i < length; i++) {
            dst[i] = src.getChar();
        }
    }

    public boolean isLongNameEntry(){
        return isLongNameEntry;
    }





}
