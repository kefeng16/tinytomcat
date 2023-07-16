import com.wkf.threadpool.BasicThreadPool;
import com.wkf.threadpool.ThreadPool;
import com.wkf.tomcat.Reactor;

import java.util.concurrent.CountDownLatch;

public class Main {
    public static volatile long i = 0;

    public static void main(String[] args) throws Exception {
        CountDownLatch l = new CountDownLatch(1);
        final ThreadPool threadPool = new BasicThreadPool(2, 32, 8, 2048);
        new Reactor(8080, threadPool).start();
        l.await();
    }
}