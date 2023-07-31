package com.wkf.lock;

import com.wkf.handler.Http500Handler;

import java.nio.channels.SocketChannel;

public class Synchronization {
    public Synchronization() {
    }

    public static boolean threadSafetyFor(SocketChannel channel, ChannelTask task) {
        synchronized (channel) {
            try {
                task.doTask(channel);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}


