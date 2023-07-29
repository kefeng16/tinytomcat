package com.wkf.lock;

import java.nio.channels.SocketChannel;

@FunctionalInterface
public interface ChannelTask {
    public boolean doTask(SocketChannel channel, Object... args) throws Exception;
}