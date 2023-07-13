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
import java.util.Map;

public class HttpRequest implements Constant {
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
    public HttpRequest(SocketChannel channel, HttpRequestHeader header, HttpRequestBody body) {
        this.channel = channel;
        this.requestHeader = header;
        this.requestBody = body;
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

    public HttpRequestBody getRequestBody() {
        return requestBody;
    }

    public SocketChannel getChannel() {
        return channel;
    }


    @Override
    public String toString() {
        return "HttpRequest{" +
                "requestHeader=" + requestHeader +
                '}';
    }
}