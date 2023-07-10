package com.wkf.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.wkf.constant.Constant;
import com.wkf.exception.ParseHttpException;
import com.wkf.handler.Http400Handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest implements Constant {
    @JsonIgnore
    public static final String GET = "GET";
    @JsonIgnore
    public static final String POST = "POST";
    @JsonInclude
    public String requestMethod;
    @JsonInclude
    public HttpRequestHeader requestHeader;
    @JsonIgnore
    public HttpRequestBody requestBody;
    @JsonIgnore
    public SocketChannel channel;
    @JsonIgnore
    public String cookie;
    @JsonInclude
    public Map<String, Object> session = new HashMap<>(32);

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

    public static HttpRequest decodeHttpRequest(HttpRequestHeader header, SocketChannel channel, ByteBuffer buf, Map<String, Object> session) throws IOException {
        HttpRequest httpRequest = new HttpRequest();
        httpRequest.channel = channel;
        httpRequest.requestHeader = header;
        httpRequest.session = session;
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

    public void writeJsonResponse(String response) throws Exception {
        if (cookie == null) {
            channel.write(ByteBuffer.wrap(String.format(json200Template, response.length(), response).getBytes()));
        } else {
            channel.write(ByteBuffer.wrap(String.format(json200TemplateWithCookie, response.length(), String.format("Set-Cookie: %s", cookie), response).getBytes()));
        }
    }

    public void writeJsonResponse(byte[] response) throws Exception {
        if (response == null) throw new Exception("data is null");
        String header = String.format(json200ResponseHeader, response.length);
        channel.write(ByteBuffer.wrap(header.getBytes(StandardCharsets.UTF_8)));
        channel.write(ByteBuffer.wrap(response));
    }

    public void writeHtmlResponse(String response) throws Exception {
        channel.write(ByteBuffer.wrap(String.format(html200Template, response.length(), response).getBytes()));
    }

    public void writeHtmlResponse(byte[] response) throws Exception {
        if (response == null) throw new Exception("data is null");
        String header = String.format(html200ResponseHeader, response.length);
        channel.write(ByteBuffer.wrap(header.getBytes(StandardCharsets.UTF_8)));
        channel.write(ByteBuffer.wrap(response));
    }

    public void writeCssResponse(String response) throws Exception {
        channel.write(ByteBuffer.wrap(String.format(css200Template, response.length(), response).getBytes()));
    }

    public void writeCssResponse(byte[] response) throws Exception {
        if (response == null) throw new Exception("data is null");
        String header = String.format(css200ResponseHeader, response.length);
        channel.write(ByteBuffer.wrap(header.getBytes(StandardCharsets.UTF_8)));
        channel.write(ByteBuffer.wrap(response));
    }

    public void writeJsResponse(String response) throws Exception {
        channel.write(ByteBuffer.wrap(String.format(js200Template, response.length(), response).getBytes()));
    }

    public void writeJsResponse(byte[] response) throws Exception {
        if (response == null) throw new Exception("data is null");
        String header = String.format(js200ResponseHeader, response.length);
        channel.write(ByteBuffer.wrap(header.getBytes(StandardCharsets.UTF_8)));
        channel.write(ByteBuffer.wrap(response));
    }

    public void writeBinaryResponse(byte[] response) throws Exception {
        if (response == null) throw new Exception("binary data is null");
        String header = String.format(binary200ResponseHeader, response.length);
        channel.write(ByteBuffer.wrap(header.getBytes(StandardCharsets.UTF_8)));
        channel.write(ByteBuffer.wrap(response));
    }

    public void writeBinaryResponse(String path) throws Exception {
        FileInputStream stream = new FileInputStream(Constant.downloadPathPrefix + path);
        FileChannel fileChannel = stream.getChannel();
        File file = new File(Constant.downloadPathPrefix + path);
        long size = file.length(), remaining = file.length();
        String header = String.format(binary200ResponseHeader, size, file.getName());
        channel.write(ByteBuffer.wrap(header.getBytes(StandardCharsets.UTF_8)));
        while (remaining > 0) {
            long n = fileChannel.transferTo(0, size, channel);
            if (n <= 0)
                break;
            remaining -= n;
        }
    }

    public void writeCommonResponseWithCookie(String headerTemplate, String path, String c) throws Exception {
        try {
            FileInputStream stream = new FileInputStream(path);
            FileChannel fileChannel = stream.getChannel();
            File file = new File(path);
            long size = file.length(), remaining = file.length();
            String cookie = "Set-Cookie: " + c;
            String header = String.format(headerTemplate, size, cookie);
            channel.write(ByteBuffer.wrap(header.getBytes(StandardCharsets.UTF_8)));
            while (remaining > 0) {
                long n = fileChannel.transferTo(0, size, channel);
                if (n <= 0)
                    break;
                remaining -= n;
            }
            fileChannel.close();
        } catch (Exception e) {
            new Http400Handler().doHandle(this);
        }
    }

    public void writeCommonResponse(String headerTemplate, String path) throws Exception {
        try {
            FileInputStream stream = new FileInputStream(path);
            FileChannel fileChannel = stream.getChannel();
            File file = new File(path);
            long size = file.length(), remaining = file.length();
            String header = String.format(headerTemplate, size);
            channel.write(ByteBuffer.wrap(header.getBytes(StandardCharsets.UTF_8)));
            while (remaining > 0) {
                long n = fileChannel.transferTo(0, size, channel);
                if (n <= 0)
                    break;
                remaining -= n;
            }
            fileChannel.close();
        } catch (Exception e) {
            new Http400Handler().doHandle(this);
        }
    }

    public void writeCommonHtmlResponse(String path) throws Exception {
        if (cookie == null) {
            writeCommonResponse(html200ResponseHeader, path);
        } else {
            writeCommonResponseWithCookie(html200ResponseHeaderWithCookie, path, cookie);
        }
    }


    public void writeCommonCssResponse(String path) throws Exception {
        writeCommonResponse(css200ResponseHeader, path);
    }

    public void writeCommonJsResponse(String path) throws Exception {
        writeCommonResponse(js200ResponseHeader, path);
    }

    public String getRequestCookie() {
        return requestHeader.header.get("Cookie");
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public void writeCommonImageResponse(String path) throws Exception {
        String suffix = "";
        if (path.endsWith("png")) {
            suffix = "png";
        } else if (path.endsWith("jpeg")) {
            suffix = "jpeg";
        } else if (path.endsWith("gif")) {
            suffix = "gif";
        } else if (path.endsWith("webp")) {
            suffix = "webp";
        } else {
            throw new Exception("invaild image request");
        }
        try {
            FileInputStream stream = new FileInputStream(path);
            FileChannel fileChannel = stream.getChannel();
            File file = new File(path);
            long size = file.length(), remaining = file.length();
            String header = String.format(image200ResponseHeader, size, suffix);
            channel.write(ByteBuffer.wrap(header.getBytes(StandardCharsets.UTF_8)));
            while (remaining > 0) {
                long n = fileChannel.transferTo(0, size, channel);
                if (n <= 0)
                    break;
                remaining -= n;
            }
            fileChannel.close();
        } catch (Exception e) {
            new Http400Handler().doHandle(this);
        }
    }

    @JsonIgnore
    public String getURLQuery(String key) {
        return requestHeader.query.get(key);
    }

    public <T> void setSession(String key, T value) {
        session.put(key, value);
    }

    public <T> T getSession(String key) {
        return (T) session.get(key);
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

    @Override
    public String toString() {
        return "HttpRequest{" +
                "requestHeader=" + requestHeader +
                ", cookie='" + cookie + '\'' +
                ", session=" + session +
                '}';
    }
}