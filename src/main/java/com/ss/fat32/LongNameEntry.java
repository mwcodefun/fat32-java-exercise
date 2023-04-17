package com.ss.fat32;

import java.util.Arrays;
import java.util.StringJoiner;

public class LongNameEntry {
    public int ldirOrd;
    public char[] ldirName1 = new char[5];
    public byte ldirAttr;
    public byte ldirType;
    public byte ldirChksum;
    public char[] ldirName2 = new char[6];
    public byte[] ldirFstClusLO = new byte[2];
    public char[] ldirName3 = new char[2];

    @Override
    public String toString() {
        return new StringJoiner(", ", LongNameEntry.class.getSimpleName() + "[", "]")
                .add("ldirOrd=" + Integer.toHexString(ldirOrd))
                .add("ldirName1=" + new String(ldirName1, 0, 5))
                .add("ldirAttr=" + ldirAttr)
                .add("ldirType=" + ldirType)
                .add("ldirChksum=" + ldirChksum)
                .add("ldirName2=" + new String(ldirName2, 0, 6))
                .add("ldirFstClusLO=" + Arrays.toString(ldirFstClusLO))
                .add("ldirName3=" + new String(ldirName3, 0, 2))
                .toString();
    }
}
