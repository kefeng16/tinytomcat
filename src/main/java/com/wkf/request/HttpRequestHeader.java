package com.wkf.request;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;

public class HttpRequestHeader {
    @JsonInclude
    public String method;
    @JsonInclude
    public String path;
    @JsonInclude
    public String version;
    @JsonInclude
    public Map<String, String> query = new HashMap<>(32);
    @JsonInclude
    public Map<String, String> header = new HashMap<>(32);

    @Override
    public String toString() {
        return "HttpRequestHeader{" +
                "method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", version='" + version + '\'' +
                ", query=" + query +
                ", header=" + header +
                '}';
    }
}

