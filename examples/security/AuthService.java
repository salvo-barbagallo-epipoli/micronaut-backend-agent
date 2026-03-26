package com.epipoli.starter.security;


import io.micronaut.security.token.jwt.generator.JwtTokenGenerator;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import org.mindrot.jbcrypt.BCrypt;
import com.epipoli.starter.exceptions.AccessNotAuthorizedException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Singleton
@Slf4j
public class AuthService {

    private final Clock             clock;
    private final JwtTokenGenerator tokenGenerator;
    private final Map<String, ApiUser> apiUserRepository;

    public AuthService(JwtTokenGenerator tokenGenerator) {
        this.tokenGenerator = tokenGenerator;
        this.apiUserRepository = Map.of(
            "test", ApiUser.builder()
                .id(1435984560876356L)
                .accessId("test")
                .secret(BCrypt.hashpw("test", BCrypt.gensalt()))
                .role(RoleType.ROLE_API)
                .build()
            );
        this.clock = Clock.systemDefaultZone();;
    }

    public Mono<AuthResponse> login(String accessId, String secret) {
        
        ApiUser apiUser = apiUserRepository.get(accessId);

        if (apiUser == null) {
            throw new AccessNotAuthorizedException();
        }

        if ( !BCrypt.checkpw(secret, apiUser.getSecret()) ) {
            throw new AccessNotAuthorizedException();
        }

        String[] roles = {apiUser.getRole()};
        Long expirationSeconds =  300L;
        Instant now = Instant.now(clock);

        Map<String, Object> claims = new HashMap<>();
        claims.put("jti", UUID.randomUUID().toString());
        claims.put("id", apiUser.getId());
        claims.put("sub", accessId);
        claims.put("iss", "demo");
        claims.put("roles", roles);
        claims.put("iat", Date.from(now));
        claims.put("exp", Date.from(now.plus(expirationSeconds, ChronoUnit.SECONDS)));

        return Mono.fromCallable(() -> tokenGenerator.generateToken(claims).get())
                .map(jwtToken -> AuthResponse.builder()
                        .access_token(jwtToken)
                        .expires_in(expirationSeconds)
                        .roles(roles)
                        .token_type("Bearer")
                        .username(accessId)
                        .build());
    }
}
