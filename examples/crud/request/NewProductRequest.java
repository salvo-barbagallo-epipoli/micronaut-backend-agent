package com.epipoli.starter.crud.request;

import com.epipoli.commons.annotation.HWAttribute;
import com.epipoli.commons.annotation.HWEntity;
import com.epipoli.commons.interfaces.IEntity;
import com.epipoli.starter.crud.model.HwEntityName;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@HWEntity(HwEntityName.PRODUCT)
@ReflectiveAccess
@Serdeable
public class NewProductRequest implements IEntity<String>{

    @HWAttribute
    @Null
    private String id;

    @HWAttribute
    @NotEmpty
    @Size(min = 13, max = 13)
    private String sku;

    @HWAttribute
    @Pattern(regexp = "[0-9a-zA-Z '-]{2,40}", message = "Invalid product name")
    private String name;

    @HWAttribute
    @Min(0)
    @NotNull
    private Double price;
}
