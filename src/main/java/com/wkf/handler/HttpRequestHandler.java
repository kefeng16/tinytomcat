package com.wkf.handler;

import com.wkf.constant.Constant;
import com.wkf.request.HttpRequest;
import com.wkf.response.HttpResponse;

public interface HttpRequestHandler extends Constant {

    public boolean hit(HttpRequest request);

    public void doHandle(HttpRequest request, HttpResponse response) throws Exception;
}
