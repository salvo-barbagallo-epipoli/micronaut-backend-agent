package com.epipoli.starter.crud.filter;

import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@ReflectiveAccess
@Serdeable
@Data
public class ProductFilter {
    private String             sku;
    @PositiveOrZero
    private Double               fromPrice;
    @PositiveOrZero
    private Double               toPrice;
}
