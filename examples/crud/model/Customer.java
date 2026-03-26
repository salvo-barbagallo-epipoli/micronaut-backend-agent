package com.epipoli.starter.crud.model;

import java.time.Instant;
import com.epipoli.commons.annotation.HWAttribute;
import com.epipoli.commons.annotation.HWEntity;
import com.epipoli.commons.interfaces.IEntity;

import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Serdeable
@ReflectiveAccess
@HWEntity(HwEntityName.CUSTOMER)
public class Customer implements IEntity<Long>{

    @HWAttribute
    @Null
    private Long id;

    @HWAttribute(unique = true)
    @Email
    @NotNull
    private String email;

    @HWAttribute
    @Null
    private Instant lastOrderDate;

    @HWAttribute
    @Pattern(regexp = "[a-zA-ZÀ-ÖØ-öø-ÿ '-]{2,40}", message = "Invalid firstname")
    @NotNull
    private String firstname;

    @HWAttribute
    @Pattern(regexp = "[a-zA-ZÀ-ÖØ-öø-ÿ '-]{2,40}", message = "Invalid lastname")
    @NotNull
    private String lastname;
    
}
