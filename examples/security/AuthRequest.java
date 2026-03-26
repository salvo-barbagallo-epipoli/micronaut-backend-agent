package com.epipoli.starter.security;

import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@ReflectiveAccess
@Serdeable.Deserializable
@Data
public class AuthRequest {

    @NotNull
    @Size(min = 3, max = 32)
    String accessId;

    @NotNull
    @Size(min = 3, max = 32)
    String secret;
}