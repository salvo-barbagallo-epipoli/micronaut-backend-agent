package com.epipoli.starter.crud.filter;

import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.Email;
import lombok.Data;

@ReflectiveAccess
@Serdeable
@Data
public class CustomerFilter {
    @Email
    private String             email;
}
