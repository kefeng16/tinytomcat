package com.wkf.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wkf.request.HttpRequest;

import java.nio.ByteBuffer;

public class Http500Handler implements HttpRequestHandler {
    public Exception exception;

    public Http500Handler(Exception exception) {
        this.exception = exception;
    }

    @Override
    public void doGet(HttpRequest request) throws Exception {
        String html = "<h1>HTTP 500 Internal Server Error</h1>";
        String response = html + "<code>" + new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(exception) + "</code>";
        request.channel.write(ByteBuffer.wrap(String.format(serverErrorResponse, response.length(), response).getBytes()));
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
