package com.wkf;

import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Synchronization {
    private static Synchronization synchronization = null;
    private SocketChannel channel;
    private Map<SocketChannel, Synchronization> map = new ConcurrentHashMap<>();

    public Synchronization(SocketChannel connection) {
        channel = connection;
    }

    public Synchronization getSynchronization(SocketChannel channel) {
        return map.get(channel);
    }

    public void setSynchronization(SocketChannel channel) {
        map.put(channel, new Synchronization(channel));
    }

}
