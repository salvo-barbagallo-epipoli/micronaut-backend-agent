package com.epipoli.starter.crud.model;

import com.epipoli.commons.annotation.HWAttribute;
import com.epipoli.commons.annotation.HWEntity;
import com.epipoli.commons.interfaces.IEntity;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;


@Data
@HWEntity(HwEntityName.PRODUCT)
@ReflectiveAccess
@Serdeable
public class Product implements IEntity<String>{

    @HWAttribute
    private String id;

    @HWAttribute
    private String sku;

    @HWAttribute
    private String name;

    @HWAttribute
    private Double price;
}