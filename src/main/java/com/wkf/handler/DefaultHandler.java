package com.wkf.handler;

import com.wkf.request.HttpRequest;

public class DefaultHandler implements HttpRequestHandler {
    @Override
    public void doGet(HttpRequest request) throws Exception {
        request.writeCommonHtmlResponse("/Users/kefeng/pages/index.html");
    }

    @Override
    public void doPost(HttpRequest request) throws Exception {
        doGet(request);
    }

    @Override
    public boolean hit(HttpRequest request) {
        return true;
    }

    @Override
    public void doHandle(HttpRequest request) throws Exception {
        doGet(request);
    }
}
