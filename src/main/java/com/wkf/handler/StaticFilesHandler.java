package com.wkf.handler;

import com.wkf.request.HttpRequest;
import com.wkf.response.HttpResponse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

public class StaticFilesHandler implements HttpRequestHandler {

    public Set<String> allowedFileTypes = new HashSet<>(Arrays.asList("html", "png", "jpeg", "webp", "js", "css", "jpg"));

    @Override
    public boolean hit(HttpRequest request) {
        String path = request.getRequestHeader().path;
        String[] split = path.split("\\.");
        String type = split[split.length - 1];
        return allowedFileTypes.contains(type) && request.getRequestHeader().getMethod().equals(GET);
    }

    @Override
    public void doHandle(HttpRequest request, HttpResponse response) throws Exception {
        response.writeBinary(root + request.getRequestHeader().getPath());
    }
}

