package com.wkf.handler;

import com.wkf.request.HttpRequest;

import java.nio.ByteBuffer;

public class Http400Handler implements HttpRequestHandler {
    @Override
    public void doGet(HttpRequest request) throws Exception {
        String json = "<h1>404 Not Found</h1>";
        request.channel.write(ByteBuffer.wrap(String.format(notFoundResponse, json.length(), json).getBytes()));
    }

    @Override
    public void doPost(HttpRequest request) throws Exception {
        doGet(request);
    }

    @Override
    public boolean hit(HttpRequest request) {
        return false;
    }

    @Override
    public void doHandle(HttpRequest request) throws Exception {
        doGet(request);
    }
}
