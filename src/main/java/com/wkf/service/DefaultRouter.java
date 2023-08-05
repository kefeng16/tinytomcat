package com.wkf.service;

import com.wkf.annotation.RequestMetadata;
import com.wkf.annotation.RequestRouter;
import com.wkf.constant.Constant;
import com.wkf.handler.DefaultHandler;
import com.wkf.request.HttpRequest;
import com.wkf.response.HttpResponse;
import com.wkf.util.Cat;
import com.wkf.util.Json;

import java.util.Date;

@RequestRouter
public class DefaultRouter implements Constant {
    public DefaultRouter() {
    }

    @RequestMetadata(path = "/index", method = GET)
    public void index(HttpRequest request, HttpResponse response) throws Exception {
        new DefaultHandler().doHandle(request, response);
    }

    @RequestMetadata(path = "/dog", method = GET)
    public void dog(HttpRequest request, HttpResponse response) throws Exception {
        request.setSession("lastReqTime", new Date());
        response.writeJson(Json.marshal(request.getSession("lastReqTime")));
    }

    @RequestMetadata(path = "/cat", method = POST)
    public void cat(HttpRequest request, HttpResponse response, Cat cat) throws Exception {
        response.writeHtml("<h1>6666</h1>");
    }
}
