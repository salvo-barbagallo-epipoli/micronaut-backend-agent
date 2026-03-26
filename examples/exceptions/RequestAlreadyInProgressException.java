package com.epipoli.starter.exceptions;

import lombok.Getter;

@Getter
public class RequestAlreadyInProgressException extends DemoException {
    public RequestAlreadyInProgressException() {
        super(429001, 419, "Request already in progress");
    }
}