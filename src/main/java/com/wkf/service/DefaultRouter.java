package com.wkf.service;

import com.wkf.annotation.RequestMetadata;
import com.wkf.annotation.RequestRouter;
import com.wkf.constant.Constant;
import com.wkf.handler.DefaultHandler;
import com.wkf.request.HttpRequest;
import com.wkf.response.HttpResponse;
import com.wkf.util.Json;

@RequestRouter
public class DefaultRouter implements Constant {
    public DefaultRouter() {
    }

    @RequestMetadata(path = "/index", method = "GET")
    public void index(HttpRequest request, HttpResponse response) throws Exception {
        new DefaultHandler().doHandle(request, response);
    }

    @RequestMetadata(path = "/login", method = "POST")
    public void login1(HttpRequest request, HttpResponse response) throws Exception {
        response.writeJson(Json.marshal(request.getRequestHeader()));
    }

}
