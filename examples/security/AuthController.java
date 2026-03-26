package com.epipoli.starter.security;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

@Controller("secure")
@ExecuteOn(TaskExecutors.IO)
public class AuthController {

    @Inject
    AuthService authService;

    @Post("login")
    @Secured({RoleType.IS_ANONYMOUS})
    public Mono<AuthResponse> login(@Body @Valid AuthRequest authRequest) {
        return authService.login(authRequest.getAccessId(), authRequest.getSecret());
    }

    @Get("authInfo")
    @Secured({RoleType.ROLE_API})
    public Authentication info(Authentication authentication ) {
        return authentication;
    }

}