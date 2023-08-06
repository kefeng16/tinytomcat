package com.wkf.response;

import com.wkf.exception.PayloadEmptyException;
import com.wkf.lock.Synchronization;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpResponse {
    public String cookie;
    private SocketChannel channel;
    private Map<String, Object> header = new HashMap<>(8);
    private StringBuilder builder = new StringBuilder("HTTP/1.1 200 OK\r\n");

    public HttpResponse(SocketChannel channel) {
        this.channel = channel;
    }

    private static String getContentType(String resourceType) {
        String contentType = "";
        switch (resourceType) {
            case "js":
                contentType = "application/javascript;charset=utf-8";
                break;
            case "html":
                contentType = "text/html;charset=utf-8";
                break;
            case "css":
                contentType = "text/css;charset=utf-8";
                break;
            case "png":
                contentType = "image/png";
                break;
            case "jpeg":
                contentType = "image/jpeg";
                break;
            case "jpg":
                contentType = "image/jpg";
                break;
            case "webp":
                contentType = "image/webp";
                break;
            case "video":
                contentType = "video/*";
                break;
            default:
                contentType = "application/octet-stream";
                break;
        }
        return contentType;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public void setResponHeader(String key, Object value) {
        header.put(key, value);
    }

    public void writeJson(String response) throws Exception {
        Synchronization.threadSafetyFor(channel, (connection, args)->{
            if (response == null) throw new PayloadEmptyException("data is null");
            byte[] payload = response.getBytes(StandardCharsets.UTF_8);
            header.put("Content-Length", payload.length);
            header.put("Content-Type", "application/json;charset=utf-8");
            if (cookie != null) {
                header.put("Cookie", cookie);
            }
            String header = buildResponseHeader();
            connection.write(ByteBuffer.wrap(header.getBytes(StandardCharsets.UTF_8)));
            connection.write(ByteBuffer.wrap(payload));
            return true;
        });
    }

    public void writeJson(byte[] response) throws Exception {
        Synchronization.threadSafetyFor(channel, (connection, args)->{
            if (response == null) throw new PayloadEmptyException("data is null");
            byte[] payload = response;
            header.put("Content-Length", payload.length);
            header.put("Content-Type", "application/json;charset=utf-8");
            if (cookie != null) {
                header.put("Cookie", cookie);
            }
            String header = buildResponseHeader();
            connection.write(ByteBuffer.wrap(header.getBytes(StandardCharsets.UTF_8)));
            connection.write(ByteBuffer.wrap(payload));
            return true;
        });
    }

    public void writeHtml(String response) throws Exception {
        Synchronization.threadSafetyFor(channel, (connection, args)->{
            if (response == null) throw new PayloadEmptyException("data is null");
            byte[] payload = response.getBytes(StandardCharsets.UTF_8);
            header.put("Content-Length", payload.length);
            header.put("Content-Type", "text/html;charset=utf-8");
            if (cookie != null) {
                header.put("Cookie", cookie);
            }
            String header = buildResponseHeader();
            connection.write(ByteBuffer.wrap(header.getBytes(StandardCharsets.UTF_8)));
            connection.write(ByteBuffer.wrap(payload));
            return true;
        });
    }

    public void writeHtml(byte[] response) throws Exception {
        Synchronization.threadSafetyFor(channel, (connection, args)->{
            if (response == null) throw new PayloadEmptyException("data is null");
            byte[] payload = response;
            header.put("Content-Length", payload.length);
            header.put("Content-Type", "text/html;charset=utf-8");
            if (cookie != null) {
                header.put("Cookie", cookie);
            }
            String header = buildResponseHeader();
            connection.write(ByteBuffer.wrap(header.getBytes(StandardCharsets.UTF_8)));
            connection.write(ByteBuffer.wrap(payload));
            return true;
        });
    }

    public void writeBinary(byte[] response) throws Exception {
        Synchronization.threadSafetyFor(channel, (connection, args)->{
            if (response == null) throw new PayloadEmptyException("data is null");
            byte[] payload = response;
            header.put("Content-Length", payload.length);
            header.put("Content-Type", "application/octet-stream");
            header.put("Content-Disposition", "attachment;filename=document");
            if (cookie != null) {
                header.put("Cookie", cookie);
            }
            String header = buildResponseHeader();
            connection.write(ByteBuffer.wrap(header.getBytes(StandardCharsets.UTF_8)));
            connection.write(ByteBuffer.wrap(payload));
            return true;
        });
    }

    public void writeBinary(String path) throws Exception {
        Synchronization.threadSafetyFor(channel, (connection, args)->{
            File file = new File(path);
            var split = file.getName().split("\\.");
            String type = getContentType(split[split.length - 1]);
            FileInputStream stream = new FileInputStream(file);
            FileChannel fileChannel = stream.getChannel();
            long size = file.length(), remaining = file.length();
            header.put("Content-Length", size);
            header.put("Content-Type", type);
            if (type.equals("application/octet-stream")) {
                header.put("Content-Disposition", "attachment;filename=" + file.getName());
            }
            String header = buildResponseHeader();
            connection.write(ByteBuffer.wrap(header.getBytes(StandardCharsets.UTF_8)));
            while (remaining > 0) {
                long n = fileChannel.transferTo(0, size, connection);
                if (n <= 0)
                    break;
                remaining -= n;
            }
            fileChannel.close();
           return true;
        });
    }

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public Map<String, Object> getHeader() {
        return header;
    }

    public void setHeader(Map<String, Object> header) {
        this.header = header;
    }

    private String buildResponseHeader() {
        for (var kv : header.entrySet()) {
            builder.append(kv.getKey() + ": " + kv.getValue() + "\r\n");
        }
        builder.append("\r\n");
        return builder.toString();
    }
}
