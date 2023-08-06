package com.wkf.cron;

import com.wkf.lock.ChannelTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import static com.wkf.lock.Synchronization.threadSafetyFor;

public class IdleConnectionCleaner extends Thread implements ChannelTask {
    private static final Queue<Connection> queue;
    private static Map<SocketChannel, Connection> map;

    static {
        queue = new PriorityQueue<>((c1, c2) -> {
            long r = c1.getLastRwAt() - c2.getLastRwAt();
            if (r < 0) return -1;
            return 1;
        });
        map = new HashMap<>();
    }

    Logger logger = LoggerFactory.getLogger("IdleConnectionCleaner");

    public void add(SocketChannel connection, Selector selector) {
        threadSafetyFor(connection, (channel, args) -> {
            var t = new Connection(System.currentTimeMillis(), channel, selector);
            queue.offer(t);
            map.put(channel, t);
            return true;
        });
    }

    public void update(SocketChannel connection) {
        threadSafetyFor(connection, (channel, args) -> {
            var conn = map.get(channel);
            if (conn == null) return false;
            conn.update();
            return true;
        });
    }

    public void remove(SocketChannel connection) {
        threadSafetyFor(connection, (channel, args) -> {
            var conn = map.get(connection);
            if (conn != null) {
                conn.lastRwAt = 0;
                map.remove(connection);
            }
            return true;
        });
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(1000 * 10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.info("connections: {}", map.size());
            while (true) {
                Connection connection = queue.peek();
                if (connection == null) break;
                if (!threadSafetyFor(queue.peek().getConnection(), this, connection)) break;
            }
        }
    }

    @Override
    public boolean doTask(SocketChannel channel, Object... args) throws Exception {
        if (channel == null) return false;
        Connection connection = (Connection) args[0];
        Selector selector = connection.getSelector();
        if (System.currentTimeMillis() - connection.getLastRwAt() > 1000 * 60 * 5) {
            try {
                channel.close();
                if (channel.keyFor(selector) != null)
                    channel.keyFor(selector).cancel();
                queue.poll();
                map.remove(connection);
                logger.info("active close connection {}", channel);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return false;
        }
        return false;
    }

    static class Connection {
        private final SocketChannel connection;
        private long lastRwAt;

        private Selector selector;

        public Connection(long lastRwAt, SocketChannel connection, Selector selector) {
            this.lastRwAt = lastRwAt;
            this.connection = connection;
            this.selector = selector;
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

        public Selector getSelector() {
            return selector;
        }

        public boolean close() {
            return (System.currentTimeMillis() - lastRwAt) > 1000 * 60 * 2; //2min
        }

        @Override
        public String toString() {
            return "Connection{" +
                    "lastRwAt=" + lastRwAt +
                    '}';
        }
    }
}