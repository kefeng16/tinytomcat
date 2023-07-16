package com.wkf.lock;

import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Synchronization {
    private static Synchronization synchronization = null;
    private SocketChannel channel;
    private Map<SocketChannel, Pair> map = new ConcurrentHashMap<>();

    public Synchronization(SocketChannel connection) {
        channel = connection;
    }

    public Pair getSynchronization(SocketChannel channel) {
        return map.get(channel);
    }

    public void setSynchronization(SocketChannel channel) {
        synchronized (channel) {
            map.put(channel, new Pair(channel));
        }
    }

    public void delSynchronization(SocketChannel channel) {
        synchronized (channel) {
            map.remove(channel);
        }
    }

}
