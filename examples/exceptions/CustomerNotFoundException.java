package com.epipoli.starter.exceptions;

import lombok.Getter;

@Getter
public class CustomerNotFoundException extends DemoException {
    public CustomerNotFoundException() {
        super(404006, 404, "Customer Not Found");
    }
}