package com.wkf.handler;

import com.wkf.constant.Constant;
import com.wkf.request.HttpRequest;

public interface HttpRequestHandler extends Constant {
    public void doGet(HttpRequest request) throws Exception;

    public void doPost(HttpRequest request) throws Exception;

    public boolean hit(HttpRequest request);

    public void doHandle(HttpRequest request) throws Exception;
}
