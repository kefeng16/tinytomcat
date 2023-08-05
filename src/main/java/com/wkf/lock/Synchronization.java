package com.wkf.lock;

import java.nio.channels.SocketChannel;

public class Synchronization {
    public Synchronization() {
    }

    public static boolean threadSafetyFor(SocketChannel channel, ChannelTask task, Object... args) {
        synchronized (channel) {
            try {
                return task.doTask(channel, args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}


