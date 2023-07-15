package com.wkf.response;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class HttpAttachment implements Comparable<HttpAttachment> {
    public Map<String, Object> session = new HashMap<>(4);

    public long lastActiveAt = 0;

    public SocketChannel connection;

    public HttpAttachment(SocketChannel channel) {
        connection = channel;
    }

    @Override
    public int compareTo(HttpAttachment o) {
        long val = o.lastActiveAt - lastActiveAt;
        if (val == 0) return 0;
        return val > 0 ? 1 : -1;
    }
}
