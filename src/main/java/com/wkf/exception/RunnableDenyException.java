package com.wkf.exception;

public class RunnableDenyException extends RuntimeException {
    public RunnableDenyException(String msg) {
        super(msg);
    }
}
