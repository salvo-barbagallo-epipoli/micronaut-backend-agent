package com.epipoli.starter.exceptions;

import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Getter;

@Getter
@Serdeable
@ReflectiveAccess
public class ErrorMessage {

    private Integer errorCode;
    private String message;

    public ErrorMessage(Integer errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }

}
