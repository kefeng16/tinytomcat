package com.wkf.tomcat;

import com.wkf.annotation.AutoCompleteEnable;
import com.wkf.annotation.RequestMetadata;
import com.wkf.annotation.RequestParameter;
import com.wkf.cron.IdleConnectionCleaner;
import com.wkf.handler.Http400Handler;
import com.wkf.handler.HttpRequestHandler;
import com.wkf.handler.StaticFilesHandler;
import com.wkf.lock.Synchronization;
import com.wkf.request.HttpRequest;
import com.wkf.request.HttpRequestHeader;
import com.wkf.response.HttpResponse;
import com.wkf.service.DefaultRouter;
import com.wkf.threadpool.ThreadPool;
import com.wkf.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.wkf.request.HttpRequest.GET;
import static com.wkf.request.HttpRequest.POST;

public class TomcatReactor extends Thread {
    final ServerSocketChannel serverSocket;
    public List<HttpRequestHandler> httpHandles;
    Selector accSelector = Selector.open();
    Selector[] rwSelectors = null;
    Logger logger = LoggerFactory.getLogger("Reactor");
    private Http400Handler default400;
    private Map<SocketChannel, Map<String, Object>> sessionMap;
    private int selectIndex = 0;
    private int subSelectorN = 4;
    private IdleConnectionCleaner cleaner;
    private ThreadPool threadPool;

    public TomcatReactor(int port, ThreadPool pool) throws Exception {
        threadPool = pool;
        // Reactor初始化
        serverSocket = ServerSocketChannel.open();
        // 非阻塞
        serverSocket.configureBlocking(false);
        serverSocket.bind(new InetSocketAddress(port));
        serverSocket.register(accSelector, SelectionKey.OP_ACCEPT);
        rwSelectors = new Selector[subSelectorN];
        for (int i = 0; i < subSelectorN; i++) {
            rwSelectors[i] = Selector.open();
            threadPool.execute(new RWHandler(rwSelectors[i]));
        }
        httpHandles = new ArrayList<>();
        default400 = new Http400Handler();
        cleaner = new IdleConnectionCleaner();
        sessionMap = new ConcurrentHashMap<>(256);
        HttpRequest.setSession(sessionMap);
        Method[] methods = DefaultRouter.class.getMethods();
        DefaultRouter router = new DefaultRouter();
        for (Method method : methods) {
            if (method.isAnnotationPresent(RequestMetadata.class)) {
                HttpRequestHandler handler = new HttpRequestHandler() {

                    @Override
                    public boolean hit(HttpRequest request) {
                        String requestMethod = method.getAnnotation(RequestMetadata.class).method();
                        String requestPath = method.getAnnotation(RequestMetadata.class).path();
                        return requestPath.equals(request.getRequestHeader().path) && requestMethod.equals(request.getRequestHeader().method);
                    }

                    @Override
                    public void doHandle(HttpRequest request, HttpResponse response) throws Exception {
                        Object[] paramList = generateParamList(request, response, method);
                        method.invoke(router, paramList);
                    }

                    @Override
                    public String toString() {
                        return method.toString();
                    }
                };
                httpHandles.add(handler);
            }
        }
        httpHandles.add(new StaticFilesHandler());
        cleaner.start();
        logger.info("Init done. Lintening on localhost:{}", port);
    }

    public synchronized Selector getNextSelector() {
        if (selectIndex == subSelectorN) {
            selectIndex = 0;
        }
        return rwSelectors[selectIndex++];
    }

    // select thread
    public void run() {
        while (true) {
            try {
                accSelector.select();
                Set<SelectionKey> selectionKeys = accSelector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    dispatch(selectionKey);
                }
                selectionKeys.clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void dispatch(SelectionKey k) {
        if (k.isAcceptable()) {
            try {
                ServerSocketChannel serverSocketChannel = (ServerSocketChannel) k.channel();
                SocketChannel connection = serverSocketChannel.accept();
                if (connection != null) {
                    connection.configureBlocking(false);
                    Selector s = getNextSelector();
                    connection.register(s, SelectionKey.OP_READ);
                    cleaner.add(connection, s);
                    s.wakeup();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Object[] generateParamList(HttpRequest request, HttpResponse response, Method method) throws Exception {
        Object[] paramList = new Object[method.getParameterCount()];
        paramList[0] = request;
        paramList[1] = response;
        switch (request.getRequestMethod()) {
            case GET: {
                var params = method.getParameterTypes();
                int index = 2;
                for (int i = index; i < params.length; i++) {
                    var param = params[i];
                    if (!param.isAnnotationPresent(RequestParameter.class)) continue;
                    var paramInstance = param.getConstructor().newInstance();
                    var fields = paramInstance.getClass().getFields();
                    for (var field : fields) {
                        if (!field.isAnnotationPresent(AutoCompleteEnable.class)) continue;
                        if (field.getType().isAssignableFrom(String.class)) {
                            field.set(paramInstance, request.getURLQuery(field.getAnnotation(AutoCompleteEnable.class).id()));
                        }
                        if (field.getType().isAssignableFrom(int.class)) {
                            field.set(paramInstance, Integer.valueOf(request.getURLQuery(field.getAnnotation(AutoCompleteEnable.class).id())));
                        }
                    }
                    paramList[index++] = paramInstance;
                }
                break;
            }
            case POST: {
                var contentType = request.getHeaders().get("Content-Type");
                if (contentType != null && contentType.contains("json")) {
                    String json = request.getRequestBodyString();
                    if (json == null) return paramList;
                    var params = method.getParameterTypes();
                    var param = params[2];
                    if (!param.isAnnotationPresent(RequestParameter.class)) return paramList;
                    var paramInstance = param.getConstructor().newInstance();
                    paramList[2] = Json.unmarshal(json, paramInstance.getClass());
                }
                break;
            }
        }
        return paramList;
    }

    // read http-request thread
    class RWHandler implements Runnable {
        private Selector selector;
        private ByteBuffer buffer = ByteBuffer.allocate(4096);

        public RWHandler(Selector selector) {
            this.selector = selector;
        }

        public void run() {
            while (true) {
                try {
                    selector.select();
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
                Set<SelectionKey> keys = selector.selectedKeys();
                for (SelectionKey key : keys) {
                    SocketChannel connection = (SocketChannel) key.channel();
                    if (connection == null) continue;
                    try {
                        readHttpRequest(connection);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                keys.clear();
            }
        }

        public void readHttpRequest(SocketChannel connection) throws Exception {
            if (connection == null) return;
            Synchronization.threadSafetyFor(connection, (channel, args) -> {
                StringBuilder builder = new StringBuilder();
                boolean finish = false;
                while (!finish) {
                    buffer.clear();
                    int n = channel.read(buffer);
                    if (n <= 0) {
                        logger.info("connection closed: {}", channel.getRemoteAddress());
                        channel.keyFor(selector).cancel();
                        sessionMap.remove(connection);
                        cleaner.remove(connection);
                        break;
                    }
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        builder.append((char) buffer.get());
                        if (builder.length() > 4) {
                            if (builder.substring(builder.length() - 4, builder.length()).equals("\r\n\r\n")) {
                                finish = true;
                                break;
                            }
                        }
                    }
                }
                String requestString = builder.toString();
                if (requestString.length() == 0) return false;
                Map<String, Object> session = sessionMap.get(channel);
                if (session == null) {
                    sessionMap.put(channel, new HashMap<>(32));
                }
                HttpRequestHeader httpHeader = HttpRequest.decodeHttpHeader(requestString);
                HttpRequest request = HttpRequest.decodeHttpRequest(httpHeader, channel, buffer);

                cleaner.update(connection);
                HttpResponse response = new HttpResponse(channel);
                logger.info("new request: {} {}", request.getRequestHeader().method, request.getRequestHeader().path);
                boolean done = false;
                for (var handler : httpHandles) {
                    if (handler.hit(request)) {
                        threadPool.execute(new Worker(request, response, handler));
                        done = true;
                        break;
                    }
                }
                if (!done) {
                    default400.doHandle(request, response);
                    logger.error("no mapping handler for request: {} {}", request.getRequestHeader().method, request.getRequestHeader().path);
                }
                return done;
            });
        }
    }

}
