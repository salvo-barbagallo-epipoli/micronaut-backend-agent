package com.epipoli.starter.exceptions;

import lombok.Getter;

@Getter
public class ProductNotFoundException extends DemoException {
    public ProductNotFoundException() {
        super(404005, 404, "Product Not Found");
    }
}