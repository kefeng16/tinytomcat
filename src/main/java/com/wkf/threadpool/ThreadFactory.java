package com.wkf.threadpool;

@FunctionalInterface
public interface ThreadFactory {
    Thread creatThread(Runnable runnable);
}
