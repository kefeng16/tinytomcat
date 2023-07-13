package com.wkf.tomcat;

import com.wkf.annotation.AutoCompleteEnable;
import com.wkf.annotation.RequestMetadata;
import com.wkf.annotation.RequestParameter;
import com.wkf.handler.Http400Handler;
import com.wkf.handler.HttpRequestHandler;
import com.wkf.request.HttpRequest;
import com.wkf.request.HttpRequestHeader;
import com.wkf.response.HttpResponse;
import com.wkf.service.DefaultRouter;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.wkf.request.HttpRequest.GET;
import static com.wkf.request.HttpRequest.POST;

public class Reactor extends Thread {

    private static final String TAG = "";
    final ServerSocketChannel serverSocket;
    public List<HttpRequestHandler> httpHandles = new ArrayList<>();
    Selector accSelector = Selector.open();
    Selector[] rwSelectors = null;
    ExecutorService service = Executors.newFixedThreadPool(4);
    Logger logger = LoggerFactory.getLogger(TAG);
    private Map<SocketChannel, Map<String, Object>> sessionMap = new ConcurrentHashMap<>(256);
    private int selectIndex = 0;
    private int subSelectorN = 4;

    public Reactor(int port) throws Exception {
        // Reactor初始化
        serverSocket = ServerSocketChannel.open();
        // 非阻塞
        serverSocket.configureBlocking(false);
        serverSocket.bind(new InetSocketAddress(port));
        serverSocket.register(accSelector, SelectionKey.OP_ACCEPT);
        rwSelectors = new Selector[subSelectorN];
        for (int i = 0; i < subSelectorN; i++) {
            rwSelectors[i] = Selector.open();
            service.submit(new RWHandler(rwSelectors[i]));
        }
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
                if (request.getHeaders().get("Content-Type").contains("json")) {
                    String json = request.getRequestBodyString();
                    if (json == null) return paramList;
                    var params = method.getParameterTypes();
                    var param = params[1];
                    if (!param.isAnnotationPresent(RequestParameter.class)) return paramList;
                    var paramInstance = param.getConstructor().newInstance();
                    paramList[1] = Json.unmarshal(json, paramInstance.getClass());
                }
                break;
            }
        }
        return paramList;
    }

    class RWHandler implements Runnable {
        Map<SocketChannel, StringBuilder> readMapping = new HashMap<>(256);
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
                    if (connection != null) {
                        try {
                            doRW(connection, new StringBuilder());
                        } catch (Exception e) {
                            try {
                                connection.close();
                            } catch (IOException ex) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                keys.clear();
            }
        }

        public void doRW(SocketChannel connection, StringBuilder builder) throws Exception {
            HttpRequest request = null;
            if (connection == null)
                return;
            StringBuilder header = builder;
            boolean finish = false;
            while (!finish) {
                buffer.clear();
                int n = connection.read(buffer);
                if (n < 0) {
                    logger.info("connection closed: {}", connection.getRemoteAddress());
                    connection.keyFor(selector).cancel();
                    break;
                }
                buffer.flip();
                while (buffer.hasRemaining()) {
                    header.append((char) buffer.get());
                    if (header.length() > 4) {
                        if (header.substring(header.length() - 4, header.length()).equals("\r\n\r\n")) {
                            finish = true;
                            break;
                        }
                    }
                }
            }
            String requestString = header.toString();
            header.delete(0, header.length());
            if (requestString.length() == 0) {
                return;
            }
            Map<String, Object> session = sessionMap.get(connection);
            if (session == null) {
                sessionMap.put(connection, new HashMap<>(32));
            }
            HttpRequestHeader httpHeader = HttpRequest.decodeHttpHeader(requestString);
            request = HttpRequest.decodeHttpRequest(httpHeader, connection, buffer);
            HttpResponse response = new HttpResponse(connection);
            logger.info("new coming request: {} {}", request.getRequestHeader().method, request.getRequestHeader().path);
            boolean done = false;
            for (var handler : httpHandles) {
                if (handler.hit(request)) {
                    new Worker(request, response, handler).start();
                    done = true;
                    break;
                }
            }
            if (!done) {
                new Http400Handler().doHandle(request, response);
                logger.error("no mapping handler for request: {} {}", request.getRequestHeader().method, request.getRequestHeader().path);
            }
        }
    }

}
