package com.ss.fat32;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.StringJoiner;

public class BpbHeader {

    public byte[] jmpBoot = new byte[3];
    public byte[] oemName = new byte[8];
    public int bytesPerSec;
    public int secPerClu;
    public int rsvdSecCnt;
    public int numFats;
    public int rootEntCnt;
    public int totSec16;
    public byte media;
    public int fatSz16;
    public int secPerTrk;

    public int fatSz32;
    public int extFlags;
    private int fsVer;

    public int rootClus;
    public int fsInfo;
    public int bkBootSec;
    public int numHeads;
    public long hiddSec;
    public long totSec32;

    @Override
    public String toString() {
        return new StringJoiner(", ", BpbHeader.class.getSimpleName() + "[", "]")
                .add("jmpBoot=" + Arrays.toString(jmpBoot))
                .add("oemName=" + Arrays.toString(oemName))
                .add("bytesPerSec=" + bytesPerSec)
                .add("secPerClu=" + secPerClu)
                .add("rsvdSecCnt=" + rsvdSecCnt)
                .add("numFats=" + numFats)
                .add("rootEntCnt=" + rootEntCnt)
                .add("totSec16=" + totSec16)
                .add("media=" + media)
                .add("fatSz16=" + fatSz16)
                .add("secPerTrk=" + secPerTrk)
                .add("fatSz32=" + fatSz32)
                .add("extFlags=" + extFlags)
                .add("fsVer=" + fsVer)
                .add("rootClus=" + rootClus)
                .add("fsInfo=" + fsInfo)
                .add("bkBootSec=" + bkBootSec)
                .toString();
    }

    public long getDataSector(){
        return this.totSec32 - this.rsvdSecCnt - this.numFats * this.fatSz32;
    }

    public long totClus(){
        return getDataSector() / this.secPerClu;
    }

    public  BpbHeader read(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        this.jmpBoot[0] = buffer.get();
        this.jmpBoot[1] = buffer.get();
        this.jmpBoot[2] = buffer.get();

        for (int i = 0; i < this.oemName.length; i++) {
            this.oemName[i] = buffer.get();
        }
        this.bytesPerSec = BufferUtils.readUint16(buffer);
        this.secPerClu = buffer.get();
        this.rsvdSecCnt = BufferUtils.readUint16(buffer);
        this.numFats = buffer.get();
        this.rootEntCnt = BufferUtils.readUint16(buffer);
        this.totSec16 = BufferUtils.readUint16(buffer);
        this.media = buffer.get();
        this.fatSz16 = BufferUtils.readUint16(buffer);
        this.secPerTrk = BufferUtils.readUint16(buffer);
        this.numHeads = BufferUtils.readUint16(buffer);
        this.hiddSec = BufferUtils.readL32(buffer);
        this.totSec32 = BufferUtils.readL32(buffer);
        byte[] totSec32 = new byte[4];
        int position = buffer.position();
        buffer.position(0);
        buffer.get(32,totSec32);
        buffer.position(position);
        System.out.println(Utils.toInt(totSec32));

        buffer.order(ByteOrder.LITTLE_ENDIAN);
        this.fatSz32 = buffer.getInt();
        this.extFlags = BufferUtils.readUint16(buffer);
        this.fsVer = BufferUtils.readUint16(buffer);
        assert this.fsVer == 0;
        this.rootClus = buffer.getInt();
        return this;
    }
}
