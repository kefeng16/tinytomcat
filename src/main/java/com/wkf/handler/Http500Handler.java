package com.wkf.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wkf.request.HttpRequest;
import com.wkf.response.HttpResponse;
import com.wkf.util.Json;

import java.nio.ByteBuffer;

public class Http500Handler implements HttpRequestHandler {
    public Exception exception;

    public Http500Handler(Exception exception) {
        this.exception = exception;
    }

    @Override
    public boolean hit(HttpRequest request) {
        return false;
    }

    @Override
    public void doHandle(HttpRequest request, HttpResponse response) throws Exception {
        String resp = "<h1>HTTP 500 Internal Server Error</h1>" + "<code>" + Json.marshal(exception) + "</code>";
        response.getChannel().write(ByteBuffer.wrap(String.format(serverErrorResponse, resp.length(), resp).getBytes()));
    }
}
