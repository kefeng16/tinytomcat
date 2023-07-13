package com.wkf.tomcat;

import com.wkf.handler.Http500Handler;
import com.wkf.handler.HttpRequestHandler;
import com.wkf.request.HttpRequest;
import com.wkf.response.HttpResponse;

class Worker extends Thread {
    HttpRequest request;
    HttpResponse response;
    HttpRequestHandler handler;

    public Worker(HttpRequest request, HttpResponse response, HttpRequestHandler handler) {
        this.request = request;
        this.response = response;
        this.handler = handler;
    }

    @Override
    public void run() {
        try {
            handler.doHandle(request, response);
        } catch (Exception e) {
            try {
                new Http500Handler(e).doHandle(request, response);
            } catch (Exception ex) {
                e.printStackTrace();
            }
        }
    }
}