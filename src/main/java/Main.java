import com.wkf.threadpool.BasicThreadPool;
import com.wkf.threadpool.ThreadPool;
import com.wkf.tomcat.TomcatReactor;

import java.util.concurrent.CountDownLatch;

public class Main {

    public static void main(String[] args) throws Exception {
        CountDownLatch l = new CountDownLatch(1);
        final ThreadPool threadPool = new BasicThreadPool(256, 1024, 512, 65535);
        new TomcatReactor(8080, threadPool).start();
        l.await();
    }
}