package com.epipoli.starter.crud.response;

import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

@Data
@ReflectiveAccess
@Serdeable.Serializable
public class ProductResponseTiny {
    private String sku;
    private Double price;
}
