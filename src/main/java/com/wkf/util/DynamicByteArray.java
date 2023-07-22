package com.wkf.util;

import java.io.ByteArrayOutputStream;

public class DynamicByteArray {
    private ByteArrayOutputStream byteArrayOutputStream;

    public DynamicByteArray() {
        byteArrayOutputStream = new ByteArrayOutputStream();
    }

    public void write(byte b) {
        byteArrayOutputStream.write(b);
    }

    public int size() {
        return byteArrayOutputStream.size();
    }

    public byte[] toByteArray() {
        return byteArrayOutputStream.toByteArray();
    }

    public byte[] toByteArray(int start, int end) {
        if (start < end) return null;
        if (end > byteArrayOutputStream.size()) return null;
        byte[] ret = new byte[end - start];
        var all = toByteArray();
        int index = 0;
        for (int i = start; i < end; i++) {
            ret[i++] = all[i];
        }
        return ret;
    }
}
