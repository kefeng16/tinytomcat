package com.wkf.handler;

import com.wkf.request.HttpRequest;
import com.wkf.response.HttpResponse;

import java.nio.ByteBuffer;

public class Http400Handler implements HttpRequestHandler {

    @Override
    public boolean hit(HttpRequest request) {
        return false;
    }

    @Override
    public void doHandle(HttpRequest request, HttpResponse response) throws Exception {
        String json = "<h1>404 Not Found</h1>";
        response.getChannel().write(ByteBuffer.wrap(String.format(notFoundResponse, json.length(), json).getBytes()));

    }
}
