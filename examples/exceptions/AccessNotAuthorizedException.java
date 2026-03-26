package com.epipoli.starter.exceptions;

public class AccessNotAuthorizedException extends DemoException {
    public AccessNotAuthorizedException() {
        super(401000, 401, "Not authorized");
    }
}
