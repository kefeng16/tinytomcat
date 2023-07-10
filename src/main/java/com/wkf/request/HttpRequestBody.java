package com.wkf.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

public class HttpRequestBody {
    @JsonInclude
    public int length;
    @JsonIgnore
    public byte[] body;

    public HttpRequestBody(int length, byte[] body) {
        this.length = length;
        this.body = body;
    }

    @Override
    public String toString() {
        return "HttpRequestBody{" +
                "length=" + length +
                ", body='" + body + '\'' +
                '}';
    }

}