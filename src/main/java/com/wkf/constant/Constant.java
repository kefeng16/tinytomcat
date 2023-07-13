package com.wkf.constant;

public interface Constant {
    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String root = "/Users/kefeng/download/";
    public static final String uploadPathPrefix = "/Users/kefeng/upload/";
    public static final String image200ResponseHeader = "HTTP/1.1 200 OK\r\nContent-Length: %d\r\nContent-Type: image/%s\r\n\r\n";
    public static final String html200Template = "HTTP/1.1 200 OK\r\nContent-Length: %d\r\nContent-Type: text/html;charset=utf-8\r\n\r\n%s";
    public static final String html200ResponseHeaderWithCookie = "HTTP/1.1 200 OK\r\nContent-Length: %d\r\nContent-Type: text/html;charset=utf-8\r\n%s\r\n\r\n";
    public static final String html200ResponseHeader = "HTTP/1.1 200 OK\r\nContent-Length: %d\r\nContent-Type: text/html;charset=utf-8\r\n\r\n";
    public static final String js200Template = "HTTP/1.1 200 OK\r\nContent-Length: %d\r\nContent-Type: text/javascript;charset=utf-8\r\n\r\n%s";
    public static final String js200ResponseHeader = "HTTP/1.1 200 OK\r\nContent-Length: %d\r\nContent-Type: text/javascript;charset=utf-8\r\n\r\n";
    public static final String css200Template = "HTTP/1.1 200 OK\r\nContent-Length: %d\r\nContent-Type: text/css;charset=utf-8\r\n\r\n%s";
    public static final String css200ResponseHeader = "HTTP/1.1 200 OK\r\nContent-Length: %d\r\nContent-Type: text/css;charset=utf-8\r\n\r\n";
    public static final String video200Template = "";
    public static final String binary200ResponseHeader = "HTTP/1.1 200 OK\r\nContent-Length: %d\r\nContent-Type: application/octet-stream\r\nContent-Disposition: attachment;filename=%s\r\n\r\n";
    public static final String text200Template = "HTTP/1.1 200 OK\r\nContent-Length: %d\r\nContent-Type: plain/text;charset=utf-8\r\n\r\n%s";
    public static final String text200ResponseHeader = "HTTP/1.1 200 OK\r\nContent-Length: %d\r\nContent-Type: plain/text;charset=utf-8\r\n\r\n";
    public static final String json200Template = "HTTP/1.1 200 OK\r\nContent-Length: %d\r\nContent-Type: application/json;charset=utf-8\r\n\r\n%s";
    public static final String json200TemplateWithCookie = "HTTP/1.1 200 OK\r\nContent-Length: %d\r\nContent-Type: application/json;charset=utf-8\r\n%s\r\n\r\n%s";
    public static final String json200ResponseHeader = "HTTP/1.1 200 OK\r\nContent-Length: %d\r\nContent-Type: application/json;charset=utf-8\r\n\r\n";
    public static final String notFoundResponse = "HTTP/1.1 404 Not Found\r\nContent-Length: %d\r\nContent-Type: text/html;charset=utf-8\r\n\r\n%s";
    public static final String serverErrorResponse = "HTTP/1.1 500 Internal Server Error\r\nContent-Length: %d\r\nContent-Type: text/html;charset=utf-8\r\n\r\n%s";
}
