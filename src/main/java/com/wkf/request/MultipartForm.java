package com.wkf.request;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

public class MultipartForm {
    private byte[] boundary;
    private byte[] contents;

    private int start;

    private Map<String, String> metadata = new HashMap<>();

    public MultipartForm(byte[] boundary, byte[] contents) {
        this.boundary = boundary;
        this.contents = contents;
    }


    /*
--X-INSOMNIA-BOUNDARY
Content-Disposition: form-data; name="field1"

value1
--X-INSOMNIA-BOUNDARY
Content-Disposition: form-data; name="field2"; filename="file.txt"
Content-Type: text/plain

File contents...
--X-INSOMNIA-BOUNDARY--
    *
    * */
    private void parse() {
        ByteArrayInputStream stream = new ByteArrayInputStream(contents, 0, contents.length - boundary.length);
        int ch = 0;
        byte a = 0, b = 0, index = 0, count = 0;
        while ((ch = stream.read()) != -1) {
            a = b;
            b = (byte) ch;
            index++;
            if (a == '\r' && b == '\n') {
                String line = new String(contents, index, count);
                if (line.equals("")) {
                    start = index;
                    return;
                } else {   //parse line
                    String[] parts = line.split("; ");
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
                }
            } else {
                count++;
            }
        }
    }
}
