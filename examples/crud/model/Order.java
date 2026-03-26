package com.epipoli.starter.crud.model;

import java.time.Instant;
import com.epipoli.commons.annotation.HWAttribute;
import com.epipoli.commons.annotation.HWEntity;
import com.epipoli.commons.interfaces.IEntity;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Data;

@Serdeable
@Data
@ReflectiveAccess
@HWEntity(HwEntityName.ORDER)
public class Order implements IEntity<Long>{

    @HWAttribute
    @Null
    private Long id;

    @HWAttribute
    @Null
    private Instant orderDate;

    @HWAttribute
    @Null
    private Double total;

    @HWAttribute(links = {HwEntityName.CUSTOMER})
    @NotNull
    private Long customerId;

    @HWAttribute(links = {HwEntityName.PRODUCT})
    @NotNull
    @NotEmpty
    private String productId;

}
