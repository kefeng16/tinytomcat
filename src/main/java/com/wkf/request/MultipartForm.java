package com.wkf.request;

import com.wkf.util.DynamicByteArray;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MultipartFormEntry {
    private final int offset;
    private final int length;
    private byte[] boundary;
    private byte[] contents;
    private Map<String, String> metadata = new HashMap<>();
    private int begin = 0, valueLen = 0;

    public MultipartFormEntry(byte[] boundary, byte[] contents, int offset, int length) {
        this.boundary = boundary;
        this.contents = contents;
        this.offset = offset;
        this.length = length;
        try {
            parse();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
    Content-Disposition: form-data; name="field1" \r\n
    \r\n
    value1
    */
    private void parse() throws Exception {
        if (boundary == null || contents == null || offset == 0 || length == 0) return;
        int a = 0, b = 0, cur = offset, v = 0;
        DynamicByteArray line = new DynamicByteArray();
        while (cur <= offset + length) {
            int ch = contents[cur];
            a = b;
            b = ch;
            if (v == 1) {
                begin = cur;
                valueLen = length - cur + offset - 1;
                // System.out.println("value: " + new String(contents, cur, length-cur+offset-1, StandardCharsets.UTF_8));
                break;
            }
            if (a == '\r' && b == '\n' && v == 0) {
                var chs = line.toByteArray();
                var str = new String(chs, 0, chs.length - 1, StandardCharsets.UTF_8);
                String[] parts = str.split("; ");
                for (int i = 0; i < parts.length; i++) {
                    String[] kv = null;
                    if (i == 0) {
                        kv = parts[i].split(": ");
                    } else {
                        kv = parts[i].split("=");
                    }
                    if (kv == null || kv.length != 2) {
                        continue;
                    }
                    metadata.put(kv[0], kv[1]);
                }
                if (str.equals("")) { //next is value
                    v = 1;
                }
                line = new DynamicByteArray();
            } else {
                line.write((byte) ch);
            }
            cur++;
        }
    }

    public boolean isFile() {
        var type = metadata.get("Content-Type");
        if (type == null) return false;
        return type.contains("octet-stream");
    }

    public String getFileName() {
        var name = metadata.get("filename");
        if (name == null) return null;
        return name.replaceAll("^\"+|\"+$", "");
    }

    public InputStream getValue() {
        return new ByteArrayInputStream(contents, begin, valueLen);
    }

    public String getName() {
        var name = metadata.get("name");
        if (name == null) return null;
        return name.replaceAll("^\"+|\"+$", "");
    }
}

class MultipartForm {
    private List<MultipartFormEntry> forms = new ArrayList<>();

    public MultipartForm() {

    }

    public MultipartForm(List<MultipartFormEntry> forms) {
        this.forms = forms;
    }

    public List<MultipartFormEntry> getForms() {
        return forms;
    }

    public synchronized void add(MultipartFormEntry entry) {
        forms.add(entry);
    }
}