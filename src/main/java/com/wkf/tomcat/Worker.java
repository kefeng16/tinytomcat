package com.wkf.tomcat;

import com.wkf.handler.Http500Handler;
import com.wkf.handler.HttpRequestHandler;
import com.wkf.request.HttpRequest;
import com.wkf.response.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Worker extends Thread {
    HttpRequest request;
    HttpResponse response;
    HttpRequestHandler handler;

    Logger logger = LoggerFactory.getLogger("");

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
                logger.error("exception occurrence when handel {}: {}", request.getRequestHeader(), e.getStackTrace());
            } catch (Exception ex) {
                e.printStackTrace();
            }
        }
    }
}