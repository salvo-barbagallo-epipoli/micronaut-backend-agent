package com.epipoli.starter.security;

import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;

@ReflectiveAccess
@Serdeable
@Data
@Builder
public class AuthResponse {

    String   username;
    String   access_token;
    String   token_type;
    Long     expires_in;
    String[] roles;
}