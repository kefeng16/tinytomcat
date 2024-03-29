package com.wkf.service;

import com.wkf.annotation.RequestMetadata;
import com.wkf.annotation.RequestRouter;
import com.wkf.constant.Constant;
import com.wkf.handler.DefaultHandler;
import com.wkf.request.HttpRequest;
import com.wkf.response.HttpResponse;
import com.wkf.util.Cat;
import com.wkf.util.Json;

@RequestRouter
public class DefaultRouter implements Constant {
    public DefaultRouter() {
    }

    @RequestMetadata(path = "/index", method = GET)
    public void index(HttpRequest request, HttpResponse response) throws Exception {
        String cookie = request.getRequestCookie();
        new DefaultHandler().doHandle(request, response);
    }

    @RequestMetadata(path = "/dog", method = GET)
    public void dog(HttpRequest request, HttpResponse response) throws Exception {
        String value = request.getRequestParam("name");
        response.writeJson(Json.marshal(request.getSession("lastReqTime")));
    }

    @RequestMetadata(path = "/cat", method = GET)
    public void cat(HttpRequest request, HttpResponse response, Cat cat) throws Exception {
        String value = request.getRequestParam("key");
        response.writeObjectJSON(cat);
    }

    @RequestMetadata(path = "/json", method = GET)
    public void json(HttpRequest request, HttpResponse response, Cat cat) throws Exception {
        String value = request.getRequestParam("wkf");
        response.writeObjectJSON(request);
    }

    @RequestMetadata(path = "/echo", method = GET)
    public void echo(HttpRequest request, HttpResponse response) throws Exception {
        response.writeObjectJSON(request);
    }
}
