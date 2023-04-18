package com.ss.fat32;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class FileCov {


    public Fat32Reader.Dir covDir(ByteBuffer byteBuffer){
        List<FatDirEntry> longNames = new ArrayList<>();
        if (byteBuffer.get(11) == Fat32Reader.ATTR_LONG_NAME){
            while(true){
                FatDirEntry fatDirEntry = new FatDirEntry(byteBuffer);
                if (fatDirEntry.isLongNameEntry()){
                    longNames.add(fatDirEntry);
                }else{
                }
            }

        }
        return null;
    }

}
