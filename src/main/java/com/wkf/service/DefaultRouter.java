package com.wkf.service;

import com.wkf.annotation.RequestMetadata;
import com.wkf.annotation.RequestRouter;
import com.wkf.constant.Constant;
import com.wkf.handler.DefaultHandler;
import com.wkf.request.HttpRequest;
import com.wkf.response.HttpResponse;
import com.wkf.util.Cat;

@RequestRouter
public class DefaultRouter implements Constant {
    public DefaultRouter() {
    }

    @RequestMetadata(path = "/index", method = GET)
    public void index(HttpRequest request, HttpResponse response) throws Exception {
        System.out.println(request.getRequestParam("name"));
        new DefaultHandler().doHandle(request, response);
    }

    @RequestMetadata(path = "/dog", method = GET)
    public void dog(HttpRequest request, HttpResponse response) throws Exception {
        response.writeJson("{}");
    }

    @RequestMetadata(path = "/cat", method = POST)
    public void cat(HttpRequest request, HttpResponse response, Cat cat) throws Exception {
//        int i = 1/0;
        response.writeHtml("<h1>6666</h1>");
    }


}
