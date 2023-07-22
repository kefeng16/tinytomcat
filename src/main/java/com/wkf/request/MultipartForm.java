package com.wkf.request;

import com.wkf.util.DynamicByteArray;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MultipartForm {
    private byte[] boundary;
    private byte[] contents;

    private final int offset;
    private final int length;
    private Map<String, String> metadata = new HashMap<>();


    public MultipartForm(byte[] boundary, byte[] contents, int offset, int length) {
        this.boundary = boundary;
        this.contents = contents;
        this.offset = offset;
        this.length = length;
//        System.out.print(new String(contents, offset, length));
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
    private void parse() throws Exception{
        if (boundary==null || contents==null ||offset==0|| length==0) return;
        int a = 0, b = 0, cur = offset;
        DynamicByteArray line = new DynamicByteArray();
        while (cur<=offset+length) {
            int ch = contents[cur];
            a = b; b = ch;
            if (a=='\r' && b=='\n') {
                var chs = line.toByteArray();
                var str = new String(chs, 0, chs.length - 1, StandardCharsets.UTF_8);
                System.out.println(str);
                line = new DynamicByteArray();
            } else {
                line.write((byte) ch);
            }
            cur++;
        }

//        ByteArrayInputStream stream = new ByteArrayInputStream(contents, offset, length);
//        var reader = new BufferedReader(new InputStreamReader(stream));
//        while (true) {
//            String line = reader.readLine();
//            System.out.print("+"+line+"+");
//            switch (line){
//                case "": {
//                    int v = stream.read();
//                    if (v==-1) {
//                        return;
//                    }
//                    char ch = (char) v;
//                    System.out.print(ch);
//                    //read value
//                    break;
//                }
//                default:{
//                    String[] parts = line.split("; ");
//                    for (int i = 0; i < parts.length; i++) {
//                        String[] kv = null;
//                        if (i == 0) {
//                            kv = parts[i].split(": ");
//                        } else {
//                            kv = parts[i].split("=");
//                        }
//                        if (kv == null || kv.length != 2) {
//                            continue;
//                        }
//                        metadata.put(kv[0], kv[1]);
//                    }
//                }
//            }
//        }
    }
}
