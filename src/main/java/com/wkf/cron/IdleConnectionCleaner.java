package com.wkf.cron;

import com.wkf.lock.ChannelTask;
import com.wkf.lock.Synchronization;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

public class IdleConnectionCleaner extends Thread implements ChannelTask {
    private static final Map<SocketChannel, ConnectionTime> map;
    private static Queue<ConnectionTime> queue;

    static {
        map = new ConcurrentHashMap<>();
        queue = new PriorityQueue<>((c1, c2) -> {
            long r = c1.getLastRwAt() - c2.getLastRwAt();
            if (r < 0) return -1;
            return 1;
        });
    }

    public static void add(SocketChannel connection) {
        synchronized (connection) {
            var t = new ConnectionTime(System.currentTimeMillis(), connection);
            map.put(connection, t);
            queue.offer(t);
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(2 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            while (true) {
                var connTime = queue.peek();
                if (connTime == null) break;
                if(!Synchronization.threadSafetyFor(connTime.getConnection(), this)) {
                    break;
                }
            }
        }
    }

    @Override
    public boolean doTask(SocketChannel channel, Object... args) throws Exception {
        if (channel == null) return true;
        ConnectionTime connTime = (ConnectionTime) args[0];
        if (System.currentTimeMillis() - connTime.getLastRwAt() > 5 * 1000 * 60) {
            try {
                channel.close();
                queue.poll();
                return true;
            } catch (IOException e) {
               throw e;
            }
        } else {
            return false;
        }
    }

}

class ConnectionTime {
    private final SocketChannel connection;
    private long lastRwAt;

    public ConnectionTime(long lastRwAt, SocketChannel connection) {
        this.lastRwAt = lastRwAt;
        this.connection = connection;
    }

    public long getLastRwAt() {
        return lastRwAt;
    }

    public SocketChannel getConnection() {
        return connection;
    }

    public void update() {
        lastRwAt = System.currentTimeMillis();
    }

    public boolean close() {
        return (System.currentTimeMillis() - lastRwAt) > 1000 * 60 * 2; //2min
    }
}