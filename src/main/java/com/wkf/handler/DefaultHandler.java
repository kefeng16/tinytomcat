package com.wkf.handler;

import com.wkf.request.HttpRequest;
import com.wkf.response.HttpResponse;

public class DefaultHandler implements HttpRequestHandler {
    @Override
    public boolean hit(HttpRequest request) {
        return true;
    }

    @Override
    public void doHandle(HttpRequest request, HttpResponse response) throws Exception {
        response.writeBinary("/Users/kefeng/pages/index.html");
    }
}
