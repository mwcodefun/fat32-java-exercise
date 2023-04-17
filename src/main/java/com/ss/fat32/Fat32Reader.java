package com.ss.fat32;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

public class Fat32Reader {

    public static byte ATTR_READ_ONLY = 0x01;
    public static byte ATTR_HIDDEN = 0x02;
    public static byte ATTR_SYSTEM = 0x04;
    public static byte ATTR_VOLUME_ID = 0x08;
    public static byte ATTR_LONG_NAME = 0x0f;

    public static class BpbHeader {
        private byte[] jmpBoot = new byte[3];
        private byte[] oemName = new byte[8];
        private int bytesPerSec;
        private int secPerClu;
        private int rsvdSecCnt;
        private int numFats;
        private int rootEntCnt;
        private int totSec16;
        private byte media;
        private int fatSz16;
        private int secPerTrk;

        private int fatSz32;
        private int extFlags;
        private int fsVer;

        private int rootClus;
        private int fsInfo;
        private int bkBootSec;

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

        public static int read4Bytes(ByteBuffer buffer) {
            byte b1 = buffer.get();
            byte b2 = buffer.get();
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            return b1 | b2 << 8;
        }


        public static BpbHeader READ_FROM_BUFFER(ByteBuffer buffer) {
            BpbHeader bpbHeader = new BpbHeader();
            bpbHeader.jmpBoot[0] = buffer.get();
            bpbHeader.jmpBoot[1] = buffer.get();
            bpbHeader.jmpBoot[2] = buffer.get();

            for (int i = 0; i < bpbHeader.oemName.length; i++) {
                bpbHeader.oemName[i] = buffer.get();
            }
            bpbHeader.bytesPerSec = read2Bytes(buffer);
            bpbHeader.secPerClu = buffer.get();
            bpbHeader.rsvdSecCnt = read2Bytes(buffer);
            bpbHeader.numFats = buffer.get();
            bpbHeader.rootEntCnt = read2Bytes(buffer);
            bpbHeader.totSec16 = read2Bytes(buffer);
            bpbHeader.media = buffer.get();
            bpbHeader.fatSz16 = read2Bytes(buffer);
            bpbHeader.secPerTrk = read2Bytes(buffer);
            buffer.position(36);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            bpbHeader.fatSz32 = buffer.getInt();
            bpbHeader.extFlags = read2Bytes(buffer);
            bpbHeader.fsVer = read2Bytes(buffer);
            assert bpbHeader.fsVer == 0;
            bpbHeader.rootClus = buffer.getInt();
            return bpbHeader;
        }
    }

    public static class Dir {
        private byte[] dirName = new byte[11];
        private byte dirAttr;
        private byte dirNTRes;
        private byte dirCrtTimeTenth;
        private int dirCrtTime;
        private int dirCrtDate;
        private int lstAccDate;
        private int fstClusHI;
        private int dirWrtTime;
        private int dirWrtDate;
        private int fstClusLO;
        private long dirFileSize;

        private String fileName;

        public int getCluNum() {
            return fstClusLO & 0xff | fstClusHI << 16;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Dir.class.getSimpleName() + "[", "]")
                    .add("fileName=" + new String(dirName, 0, 8))
                    .add("file name suffix=" + new String(dirName, 8, 3))
                    .add("dirAttr=0x" + Integer.toHexString(dirAttr))
                    .add("dirNTRes=" + dirNTRes)
                    .add("dirCrtTimeTenth=" + dirCrtTimeTenth)
                    .add("dirCrtTime=" + dirCrtTime)
                    .add("dirCrtDate=" + dirCrtDate)
                    .add("lstAccDate=" + lstAccDate)
                    .add("fstClusHI=" + fstClusHI)
                    .add("dirWrtTime=" + dirWrtTime)
                    .add("dirWrtDate=" + dirWrtDate)
                    .add("fstClusLO=" + fstClusLO)
                    .add("dirFileSize=" + dirFileSize)
                    .toString();
        }
    }

    MappedByteBuffer mappedByteBuffer;

    BpbHeader bpbHeader;

    private int maxCluster;
    private static int BAD_CLUSTER_ID = 0xffffff7;
    FileChannel fileChannel;

    RandomAccessFile randomAccessFile;



    public Dir readDirWithLongName(ByteBuffer b) {
        ArrayList<LongNameEntry> longNameEntries = new ArrayList<>();
//        if (longNameEntry.ldirOrd  != 0x40){
//            throw new IllegalStateException("read not first long name entry");
//        }
        LongNameEntry last = null;
        LongNameEntry entry = null;
        while (true) {
            int startp = b.position();
            try {
                entry = readLongNameDir(b);
                if (last != null){
                    int order = last.ldirOrd & 0x1111111;
                    if (order - entry.ldirOrd == 1){
                        longNameEntries.add(last);
                    }
                }
                assert entry.ldirType == 0;
                assert entry.ldirFstClusLO[0] == 0;
                assert entry.ldirFstClusLO[1] == 0;
                if (entry.ldirOrd == (0x01 | 0x40)) {
                    break;
                }
                if (entry.ldirOrd == 0x01) {
                    break;
                }
                last = entry;
            } catch (Exception e) {
                b.position(startp);
                break;
            }
        }
        if (entry != null){
            longNameEntries.add(entry);
        }
        Dir dir = readDirectory(b,null);

        if (longNameEntries.size() > 0) {
            StringBuffer stringBuffer = new StringBuffer();
            for (int i = longNameEntries.size() - 1; i >= 0; i--) {
                LongNameEntry e = longNameEntries.get(i);
                stringBuffer.append(e.ldirName1);
                stringBuffer.append(e.ldirName2);
                stringBuffer.append(e.ldirName3);
            }
            dir.fileName = stringBuffer.toString().trim();
        }
        return dir;
    }

    public List<Dir> readDirContent(Dir dir) {
        int cluster = dir.getCluNum();
        ByteBuffer byteBuffer = readClusterContent(cluster);
        int entrySize = byteBuffer.capacity() / 32;
        List<Dir> ans = new ArrayList<>();
        int i = 0;
        List<FatDirEntry> entries = new ArrayList<>();
        while (i < entrySize) {
            ByteBuffer entry = byteBuffer.slice(i * 32, 32);
            byteBuffer.position(byteBuffer.position() + 32);
            i++;
            FatDirEntry fatDirEntry = new FatDirEntry(entry);
            entry.position(0);
            if (fatDirEntry.isLongNameEntry()) {
                if (entries == null) {
                    entries = new ArrayList<>();
                }
                entries.add(fatDirEntry);
            } else {
                Dir dir1 = readDirectory(entry, entries);
                if (entries != null && entries.size() != 0) {
                    entries = new ArrayList<>();
                }
                ans.add(dir1);
            }
        }
        return ans;
    }


    public void read(String path) throws IOException {
        this.randomAccessFile = new RandomAccessFile(path, "rw");
        this.fileChannel = this.randomAccessFile.getChannel();
        this.mappedByteBuffer = this.fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, this.fileChannel.size());
        MappedByteBuffer firstSector = this.mappedByteBuffer.slice(0, 512);
        this.bpbHeader = BpbHeader.READ_FROM_BUFFER(firstSector);
        this.maxCluster = 0;
        Dir dir = readRootDir();
        System.out.println("root dir=" + dir + "\nrootClu=" + dir.getCluNum());
        List<Dir> dirs = readDirContent(dir);
        for (Dir d : dirs) {
            System.out.println(d.fileName);
        }
    }

    private Dir readRootDir() {
        ByteBuffer byteBuffer = readClusterContent(bpbHeader.rootClus);
        return readDirectory(byteBuffer,null);
    }

    public static int read2Bytes(ByteBuffer buffer) {
        byte b1 = buffer.get();
        byte b2 = buffer.get();
        int ans = b2 & 0xff;
        return ans << 8 | b1 & 0xff;
    }

    public long read4bytes(ByteBuffer b) {
        byte b1 = b.get();
        byte b2 = b.get();
        byte b3 = b.get();
        byte b4 = b.get();
        long ans = b4 & 0xff;
        ans = ans << 8 | b3 & 0xff;
        ans = ans << 8 | b2 & 0xff;
        return ans << 8 | b1 & 0xff;
    }

    private boolean readToChars(char[] dst, ByteBuffer src, int length) {
        for (int i = 0; i < length; i++) {
            char aChar = src.getChar();
            if (aChar == '\u0000') {
                src.position(src.position() + (length - i - 1) * 2);
                return true;
            }
            dst[i] = aChar;
        }
        return false;
    }

    private void readToBytes(byte[] dst, ByteBuffer src, int length) {
        for (int i = 0; i < length; i++) {
            dst[i] = src.get();
        }
    }

    public LongNameEntry readLongNameDir(ByteBuffer b) {
        LongNameEntry longNameEntry = new LongNameEntry();
        b.order(ByteOrder.LITTLE_ENDIAN);
        longNameEntry.ldirOrd = (b.get() & 0xff);
        if (longNameEntry.ldirOrd <= 0) {
            throw new IllegalStateException();
        }
        assert longNameEntry.ldirOrd > 0;
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
        return longNameEntry;
    }

    public Dir readDirectory(ByteBuffer b,List<FatDirEntry> longNameEntries) {
        Dir dir = new Dir();
        b.order(ByteOrder.LITTLE_ENDIAN);
        readToBytes(dir.dirName, b, 11);
        dir.dirAttr = b.get();
        dir.dirNTRes = b.get();
        assert dir.dirNTRes == 0;
        dir.dirCrtTimeTenth = b.get();
        dir.dirCrtTime = read2Bytes(b);
        dir.dirCrtDate = read2Bytes(b);
        dir.lstAccDate = read2Bytes(b);
        dir.fstClusHI = read2Bytes(b);
        dir.dirWrtTime = read2Bytes(b);
        dir.dirWrtDate = read2Bytes(b);
        dir.fstClusLO = read2Bytes(b);
        dir.dirFileSize = b.getInt();
        if (longNameEntries != null && !longNameEntries.isEmpty()){
            StringBuffer stringBuffer = new StringBuffer();
            for (int i = longNameEntries.size(); i > 0; i--) {
                LongNameEntry longNameEntry = longNameEntries.get(i - 1).longNameEntry;
                stringBuffer.append(longNameEntry.ldirName1);
                stringBuffer.append(longNameEntry.ldirName2);
                stringBuffer.append(longNameEntry.ldirName3);
            }
            dir.fileName = stringBuffer.toString();
        }else{
            dir.fileName = new String(dir.dirName);
        }
        assert dir.dirNTRes == 0;
        return dir;
    }

    public int readFatEntry(int clusterNum) {
        int fatOffset = clusterNum * 4;
        int fatSector = bpbHeader.rsvdSecCnt + (fatOffset / bpbHeader.bytesPerSec);
        int fatOffsetInSector = fatOffset % bpbHeader.bytesPerSec;
        ByteBuffer byteBuffer = this.mappedByteBuffer.slice(fatSector * bpbHeader.bytesPerSec, bpbHeader.bytesPerSec);
        byteBuffer.position(fatOffsetInSector);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byte b1 = byteBuffer.get();
        byte b2 = byteBuffer.get();
        byte b3 = byteBuffer.get();
        byte b4 = byteBuffer.get();
        return b1 & 0xff | (b2 & 0xff) << 8 | (b3 & 0xff) << 16 | (b4 & 0xf) << 24;
    }

    public ByteBuffer readClusterContent(int n) {
        int dataSector = this.bpbHeader.rsvdSecCnt + bpbHeader.numFats * bpbHeader.fatSz32;
        dataSector = (n - 2) * bpbHeader.secPerClu + dataSector;
        return this.mappedByteBuffer.slice(dataSector * bpbHeader.bytesPerSec, bpbHeader.bytesPerSec * bpbHeader.secPerClu);
    }

    public void close() throws IOException {
        this.fileChannel.close();
        this.randomAccessFile.close();
    }

    public static void main(String[] args) throws IOException {
        Fat32Reader fat32Reader = new Fat32Reader();
        fat32Reader.read("/Users/ss/fat32/img");
    }

}
