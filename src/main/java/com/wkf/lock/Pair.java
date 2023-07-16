package com.wkf.lock;

import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class Pair {
    private static int UNLOCKED = 1;
    private static int LOCKED = 0;
    private SocketChannel channel;
    private AtomicInteger flag = new AtomicInteger(); // 0: hold 1: free

    private AtomicBoolean hungry = new AtomicBoolean();
    private ReentrantLock lock = new ReentrantLock();

    public Pair(SocketChannel channel) {
        this.channel = channel;
        flag.set(UNLOCKED);
        hungry.set(false);
    }

    public void Lock() {
        lock.lock();
    }

    public void Unlock() {
        lock.unlock();
    }

}
