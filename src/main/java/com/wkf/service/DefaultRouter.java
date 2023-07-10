package com.wkf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wkf.annotation.RequestMetadata;
import com.wkf.annotation.RequestRouter;
import com.wkf.constant.Constant;
import com.wkf.handler.DefaultHandler;
import com.wkf.request.HttpRequest;
import com.wkf.util.Json;

import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;

@RequestRouter
public class DefaultRouter implements Constant {
    public DefaultRouter() {
    }

    @RequestMetadata(path = "/index", method = "GET")
    public void index(HttpRequest request) throws Exception {
        new DefaultHandler().doHandle(request);
    }

    @RequestMetadata(path = "/", method = "GET")
    public void index0(HttpRequest request) throws Exception {
        new DefaultHandler().doHandle(request);
    }

    @RequestMetadata(path = "/login", method = "POST")
    public void login1(HttpRequest request) throws Exception {
        request.writeJsonResponse(new ObjectMapper().writeValueAsString(request));
    }

    @RequestMetadata(path = "/login", method = "GET")
    public void login2(HttpRequest request) throws Exception {
        request.setCookie(UUID.randomUUID().toString());
        request.setSession("params", request.getRequestBodyString());
        request.writeJsonResponse(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(request));
    }

    @RequestMetadata(path = "/header", method = "GET")
    public void header(HttpRequest request) throws Exception {
        request.writeJsonResponse(Json.marshal(request.getHeaders()));
    }

    @RequestMetadata(path = "/download", method = "GET")
    public void download(HttpRequest request) throws Exception {
        String fileName = request.getURLQuery("name");
        request.writeBinaryResponse(fileName);
    }

    @RequestMetadata(path = "/upload", method = "POST")
    public void upload(HttpRequest request) throws Exception {
        String fileName = request.getURLQuery("name");
        if (fileName == null) throw new Exception("fileName not set");
        byte[] contents = request.requestBody.body;
        if (contents == null) throw new Exception("file is null");
        File file = new File(Constant.uploadPathPrefix + fileName);
        FileOutputStream stream = new FileOutputStream(file);
        stream.write(contents, 0, contents.length);
        request.writeJsonResponse(Json.marshal(request.requestHeader));
    }

}
