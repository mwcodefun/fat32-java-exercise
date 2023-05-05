package com.ss.fat32;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class Fat32System {

    public static byte ATTR_READ_ONLY = 0x01;
    public static byte ATTR_HIDDEN = 0x02;
    public static byte ATTR_SYSTEM = 0x04;
    public static byte ATTR_VOLUME_ID = 0x08;
    public static byte ATTR_LONG_NAME = 0x0f;


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
            return fstClusLO & 0xffff | (fstClusHI & 0xffff) << 16;
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

    private BpbHeader bpbHeader;

    private long maxCluster;
    private static int BAD_CLUSTER_ID = 0xffffff7;
    FileChannel fileChannel;

    RandomAccessFile randomAccessFile;

    public static final int FAT_ENTRY_SIZE = 32;

    public List<Dir> readClusterDir(int cluster) {
        ByteBuffer byteBuffer = readClusterContent(cluster);
        int clusCnt = byteBuffer.capacity() / 32;
        List<Dir> ans = new ArrayList<>();
        int i = 0;
        List<FatDirEntry> entries = new ArrayList<>();
        while (i < clusCnt) {
            ByteBuffer entry = byteBuffer.slice(i * FAT_ENTRY_SIZE, FAT_ENTRY_SIZE);
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
                if (dir1.dirName[0] == 0xe5){
                    //dir is free
                    continue;
                }
                if (dir1.dirName[0] < 0x20){
                    continue;
                }
                if (dir1.dirName[0] == 0x00){
                    //all follow this entry dir is free
                    break;
                }
                ans.add(dir1);
            }
        }
        return ans;
    }

    public void writeContent(Dir dir,FileChannel fileChannel) throws IOException {
        assert (dir.dirAttr & 0x10) != 1;
        int cluNum = dir.getCluNum();
        long size = dir.dirFileSize;
        while (true){
            if (cluNum == 0x0){
                break;
            }
            if(cluNum == BAD_CLUSTER_ID){
                break;
            }
            if (cluNum >= 0x02 && cluNum <= this.maxCluster){
                ByteBuffer byteBuffer = readClusterContent(cluNum);
                if (byteBuffer.capacity() > size){
                    byteBuffer.limit((int) size);
                    size = 0;
                }else{
                    size = size - byteBuffer.capacity();
                }
                fileChannel.write(byteBuffer,fileChannel.size());
                cluNum = readFatEntry(cluNum);
            }else{
                break;
            }
        }
    }

    public void saveToAnotherFile(Dir dir, File file){
        try {
            FileOutputStream fos = new FileOutputStream(file);
            FileChannel channel = fos.getChannel();
            writeContent(dir,channel);
            channel.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public List<Dir> readDirContent(Dir dir){
        int clusterNum = dir.getCluNum();
        List<Dir> dirs = new ArrayList<>();
        while (true){
            if (clusterNum == 0x0){
                break;
            }
            if(clusterNum == BAD_CLUSTER_ID){
                break;
            }
            if (clusterNum >= 0x02 && clusterNum <= this.maxCluster){
                dirs.addAll(readClusterDir(clusterNum));
                clusterNum = readFatEntry(clusterNum);
            }else{
                break;
            }
        }
        return dirs;
    }


    public void read(String path) throws IOException {
        this.randomAccessFile = new RandomAccessFile(path, "rw");
        this.fileChannel = this.randomAccessFile.getChannel();
        this.mappedByteBuffer = this.fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, this.fileChannel.size());
        MappedByteBuffer firstSector = this.mappedByteBuffer.slice(0, 512);
        this.bpbHeader = new BpbHeader();
        this.bpbHeader.read(mappedByteBuffer);
        this.maxCluster = this.bpbHeader.totClus();
        assert this.maxCluster > 0;
        Dir dir = readRootDir();
        System.out.println("root dir=" + dir + "\nrootClu=" + dir.getCluNum());
        List<Dir> dirs1 = readDirContent(dir);
        for (Dir dir1 : dirs1) {
            System.out.println(dir1.fileName + ((dir1.dirAttr & 0x10) == 1 ? "is a dir": "is a file"));
            if (dir1.fileName.equals("vXyZkWFGf486oGM.bmp")){
                File file = new File("vXyZkWFGf486oGM.bmp");
                saveToAnotherFile(dir1,file);
            }
        }
        System.out.println("total=" + dirs1.size());
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

    private void readToBytes(byte[] dst, ByteBuffer src, int length) {
        for (int i = 0; i < length; i++) {
            dst[i] = src.get();
        }
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
            dir.fileName = getFileName(longNameEntries);
        }else{
            dir.fileName = getShortName(dir);
        }
        assert dir.dirNTRes == 0;
        return dir;
    }

    public String getFileName(List<FatDirEntry> list){
        StringBuffer sb = new StringBuffer();
        if (list != null && !list.isEmpty()) {
            for (int i = list.size(); i > 0; i--) {
                char[] chars = new char[13];
                LongNameEntry e = list.get(i - 1).longNameEntry;
                System.arraycopy(e.ldirName1, 0, chars, 0, 5);
                System.arraycopy(e.ldirName2, 0, chars, 5, 6);
                System.arraycopy(e.ldirName3, 0, chars, 11, 2);
                for (char c : chars) {
                    if (c == '\u0000') {
                        break;
                    }
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    public String getShortName(Dir dir){
        byte[] bytes = dir.dirName;
        if (bytes[0] == 0){
            return null;
        }
        if (bytes[0] == 0xe5){
            return null;
        }
        if (bytes[0] == 0x05){
            bytes[0] = (byte) 0xe5;
        }
        String main = new String(bytes,0,8);
        String ext = new String(bytes,8,3);
        if (ext.trim().length() > 0){
            return main + '.' + ext;
        }
        return main;

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

    private int[] clusterTag;

    public void fileCov(){
        this.clusterTag = new int[(int)maxCluster + 1];
        for (int i = 2;i < maxCluster;i++){
            ByteBuffer byteBuffer = readClusterContent(i);
            String s = new String(byteBuffer.array());
            byte[] header = new byte[2];
            byteBuffer.get(0,header);
            if (header[0] == 0x42 && header[1] == 0x4d) {
                System.out.println("find bmp header");
                clusterTag[i] = 2;
            }
            if (s.contains("bmp")){
                System.out.println("find dir");
                clusterTag[i] = 1;
            }
        }
        for (int i = 2;i < maxCluster;i++){
            if (clusterTag[i] == 1){
                //maybe long name last entry
                ByteBuffer byteBuffer = readClusterContent(i);
                byte attr = byteBuffer.get(12);
                if (attr == ATTR_LONG_NAME){
                    //this is last long name
                    //read next until dir struct entry
                }

            }
        }
    }


    public static void main(String[] args) throws IOException {
        Fat32System fat32System = new Fat32System();
        fat32System.read("/Users/ss/fat32/img");
    }

}
