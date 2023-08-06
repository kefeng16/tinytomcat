package com.wkf.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.wkf.constant.Constant;
import com.wkf.exception.ParseHttpException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class HttpRequest implements Constant {
    private static Map<SocketChannel, Map<String, Object>> session;
    @JsonInclude
    private String requestMethod;
    @JsonInclude
    private HttpRequestHeader requestHeader;
    @JsonIgnore
    private HttpRequestBody requestBody;
    @JsonIgnore
    private SocketChannel channel;

    public HttpRequest() {
    }

    @JsonIgnore
    public HttpRequest(SocketChannel channel, HttpRequestHeader header, HttpRequestBody body, Map<SocketChannel, Map<String, Object>> session) {
        this.channel = channel;
        this.requestHeader = header;
        this.requestBody = body;
        this.session = session;
    }

    public static void setSession(Map<SocketChannel, Map<String, Object>> session) {
        HttpRequest.session = session;
    }

    @JsonIgnore
    public static HttpRequestHeader decodeHttpHeader(String header) throws ParseHttpException, UnsupportedEncodingException {
        HttpRequestHeader r = new HttpRequestHeader();
        String[] lines = header.split("\r\n");
        if (lines.length == 0)
            throw new ParseHttpException(header);

        String line1 = lines[0];
        String[] arr = line1.split(" ");
        if (arr.length != 3)
            throw new ParseHttpException(header);
        r.method = arr[0];
        String[] parts = arr[1].split("\\?");
        r.path = parts[0];
        if (parts.length == 2) {
            String[] params = parts[1].split("&");
            for (String each : params) {
                String[] kv = each.split("=");
                if (kv.length != 2) {
                    throw new ParseHttpException(header);
                }
                r.query.put(URLDecoder.decode(kv[0], "UTF-8"), URLDecoder.decode(kv[1], "UTF-8"));
            }
        }
        r.version = arr[2];
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.length() == 0) continue;
            String[] kv = line.split(": ", 2);
            if (kv.length != 2)
                throw new ParseHttpException(header);
            r.header.put(kv[0], kv[1]);
        }
        return r;
    }

    public static HttpRequest decodeHttpRequest(HttpRequestHeader header, SocketChannel channel, ByteBuffer buf) throws IOException {
        HttpRequest httpRequest = new HttpRequest();
        httpRequest.channel = channel;
        httpRequest.requestHeader = header;
        switch (header.method) {
            case GET:
                httpRequest.requestMethod = GET;
                break;
            case POST:
                httpRequest.requestMethod = POST;
                break;
        }
        String len = header.header.get("Content-Length");
        if (len == null) return httpRequest;
        Integer bodySize = Integer.valueOf(len);
        if (bodySize == null) {
            return httpRequest;
        }
        int remaining = bodySize - buf.position(), r = 0;
        byte[] bytes = new byte[bodySize];
        while (buf.hasRemaining()) {
            bytes[r++] = buf.get();
        }
        while (remaining > 0) {
            buf.clear();
            int bytesRead = channel.read(buf);
            if (bytesRead <= 0) {
                break;
            }
            buf.flip();
            while (buf.hasRemaining()) {
                bytes[r++] = buf.get();
            }
            remaining -= bytesRead;
        }
        HttpRequestBody body = new HttpRequestBody(bodySize, bytes);
        httpRequest.requestBody = body;

        return httpRequest;
    }

    public String getRequestCookie() {
        return requestHeader.header.get("Cookie");
    }

    @JsonIgnore
    public String getURLQuery(String key) {
        return requestHeader.query.get(key);
    }

    @JsonIgnore
    public Map<String, String> getHeaders() {
        return requestHeader.header;
    }

    @JsonIgnore
    public byte[] getRequestBodyBytes() {
        return requestBody.body;
    }

    @JsonIgnore
    public String getRequestBodyString() {
        if (requestBody == null) return "";
        if (requestBody.body == null) return "";
        return new String(requestBody.body, StandardCharsets.UTF_8);
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public HttpRequestHeader getRequestHeader() {
        return requestHeader;
    }

    public String getRequestParam(String key) {
        var type = getRequestHeader().getHeaderValue("Content-Type");
        if (type == null) return null;
        if (type.contains("x-www-form-urlencoded")) {
            String body = getRequestBodyString();
            if (body == null || body.equals("")) return "";
            var parts = body.split("\\&");
            for (var part : parts) {
                var kv = part.split("\\=");
                if (kv.length != 2) continue;
                kv[0] = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                kv[1] = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                if (kv[0].equals(key)) return kv[1];
            }
        } else if (type.contains("multipart/form-data")) {
            String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
            int index = type.indexOf("boundary=");
            if (index != -1) {
                boundary = "--" + type.substring(index + 9);
                try {
                    parseMultipartForm(requestBody.body, boundary.getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return "";
    }

    public void parseMultipartForm(byte[] content, byte[] boundary) throws Exception {
        var form = new MultipartForm();
        List<MultipartForm> parts = new ArrayList<>(4);
        int index = boundary.length + 1, a = 0, b = 0, c = 0, size = 0;
        int cur = index + 1;
        int offset = cur, length = 0;
        byte[] fifo = new byte[boundary.length];
        boolean inSearch = false;
        while (cur < content.length) {
            int ch = content[cur];
            char t = (char) ch;
            a = b;
            b = c;
            c = ch;
            if (a == '\r' && b == '\n') {
                inSearch = true;
                size = 0;
            }
            if (inSearch) {
                if (size < fifo.length) {
                    fifo[size++] = (byte) ch;
                }
                if (size == fifo.length) {
                    if (Arrays.equals(fifo, boundary)) {
                        length = cur - offset - boundary.length + 1;
                        form.add(new MultipartFormEntry(boundary, content, offset, length));
                        char x = (char) content[++cur];
                        char y = (char) content[++cur];
                        if (x == '-' && y == '-') return;
                        if (x != '\r') throw new ParseHttpException("bad MultipartForm");
                        if (y != '\n') throw new ParseHttpException("bad MultipartForm");
                        inSearch = false;
                        offset = cur + 1;
                    } else {
                        inSearch = false;
                        size = 0;
                    }
                }
            }
            cur++;
        }
    }

    public HttpRequestBody getRequestBody() {
        return requestBody;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public Object getSession(String key) {
        if (session.get(channel) == null) return null;
        return session.get(channel).get(key);
    }

    public void setSession(String key, Object value) {
        if (session.get(channel) == null) return;
        session.get(channel).put(key, value);
    }

    @Override
    public String toString() {
        return "HttpRequest{" +
                "requestHeader=" + requestHeader +
                '}';
    }
}