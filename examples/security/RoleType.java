package com.epipoli.starter.security;

import io.micronaut.security.rules.SecurityRule;

public interface RoleType {
    public String ROLE_API         = "ROLE_API";
    public String IS_ANONYMOUS     = SecurityRule.IS_ANONYMOUS;
    public String IS_AUTHENTICATED = SecurityRule.IS_AUTHENTICATED;

}