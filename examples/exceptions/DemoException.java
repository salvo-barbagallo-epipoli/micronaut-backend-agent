package com.epipoli.starter.exceptions;

import lombok.Getter;

@Getter
public class DemoException extends RuntimeException {
    private int        code;
    private int        httpStatus;

    public DemoException(int code, int httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }
}