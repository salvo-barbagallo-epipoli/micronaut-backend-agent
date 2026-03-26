# Security Example - Authentication & Authorization with JWT

Questo esempio dimostra come implementare autenticazione e autorizzazione basate su JWT (JSON Web Tokens) in Micronaut, con gestione di ruoli e token con scadenza.

## Concetti Fondamentali

### Autenticazione vs Autorizzazione

- **Autenticazione**: Verifica l'identità dell'utente (chi sei?)
- **Autorizzazione**: Verifica i permessi dell'utente (cosa puoi fare?)

### JWT (JSON Web Token)

Un JWT è un token self-contained che contiene:
- **Header**: Tipo di token e algoritmo di firma
- **Payload**: Dati utente (claims)
- **Signature**: Firma digitale per verificare l'integrità

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.
eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.
SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

## Architettura

```
┌─────────────────────────────────────┐
│   Client                            │
│   1. POST /secure/login (username/pwd)
└─────────┬───────────────────────────┘
          │
          ▼
┌─────────────────────────────────────┐
│   AuthController                    │
│   - login endpoint                  │
│   - authInfo endpoint               │
└─────────┬───────────────────────────┘
          │
          ▼
┌─────────────────────────────────────┐
│   AuthService                       │
│   - Verifica credenziali (BCrypt)   │
│   - Genera JWT Token                │
│   - Definisce claims                │
└─────────┬───────────────────────────┘
          │
  ┌───────┴───────────┐
  ▼                   ▼
┌──────────────┐  ┌──────────────────┐
│ ApiUser DB   │  │ JwtTokenGenerator│
│              │  │                  │
└──────────────┘  └──────────────────┘
          │
          ▼
┌─────────────────────────────────────┐
│   AuthResponse                      │
│   - access_token (JWT)              │
│   - expires_in                      │
│   - roles                           │
└─────────┬───────────────────────────┘
          │
          ▼
┌─────────────────────────────────────┐
│   Client (con token)                │
│   GET /secure/authInfo              │
│   Authorization: Bearer <token>     │
└─────────────────────────────────────┘
```

## 1. Modellazione dell'Utente - ApiUser

```java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@HWEntity("api_user")
public class ApiUser implements IEntity<Long> {

    @HWAttribute
    private Long id;

    @HWAttribute
    private String accessId;      // Username

    @HWAttribute
    private String role;          // ROLE_API

    @HWAttribute
    private String secret;        // Password con hash BCrypt
}
```

**Campi importanti:**
- `id`: Identificativo unico dell'utente
- `accessId`: Nome utente (username)
- `role`: Ruolo autorizzativo (ROLE_API, ROLE_ADMIN, etc.)
- `secret`: Password con hash BCrypt (mai in chiaro!)

## 2. Tipi di Ruoli - RoleType

```java
public interface RoleType {
    public String ROLE_API         = "ROLE_API";
    public String IS_ANONYMOUS     = SecurityRule.IS_ANONYMOUS;
    public String IS_AUTHENTICATED = SecurityRule.IS_AUTHENTICATED;
}
```

**Costanti disponibili:**
- `ROLE_API`: Accesso all'API
- `IS_ANONYMOUS`: Pubblico (nessun token necessario)
- `IS_AUTHENTICATED`: Qualsiasi utente autenticato

### Aggiungere Nuovi Ruoli

```java
public interface RoleType {
    public String ROLE_API       = "ROLE_API";
    public String ROLE_ADMIN     = "ROLE_ADMIN";
    public String ROLE_MANAGER   = "ROLE_MANAGER";
    public String ROLE_USER      = "ROLE_USER";
    public String IS_ANONYMOUS   = SecurityRule.IS_ANONYMOUS;
    public String IS_AUTHENTICATED = SecurityRule.IS_AUTHENTICATED;
}
```

## 3. Request/Response DTOs

### AuthRequest

```java
@ReflectiveAccess
@Serdeable.Deserializable
@Data
public class AuthRequest {

    @NotNull
    @Size(min = 3, max = 32)
    String accessId;        // Username

    @NotNull
    @Size(min = 3, max = 32)
    String secret;          // Password
}
```

### AuthResponse

```java
@ReflectiveAccess
@Serdeable
@Data
@Builder
public class AuthResponse {

    String   username;      // Username
    String   access_token;  // JWT Token
    String   token_type;    // "Bearer"
    Long     expires_in;    // Secondi fino alla scadenza
    String[] roles;         // Ruoli dell'utente
}
```

**Esempio di risposta:**
```json
{
  "username": "test",
  "access_token": "eyJhbGc...",
  "token_type": "Bearer",
  "expires_in": 300,
  "roles": ["ROLE_API"]
}
```

## 4. Autenticazione con BCrypt

### AuthService - Login

```java
@Singleton
@Slf4j
public class AuthService {

    private final JwtTokenGenerator tokenGenerator;
    private final Map<String, ApiUser> apiUserRepository;

    public AuthService(JwtTokenGenerator tokenGenerator) {
        this.tokenGenerator = tokenGenerator;
        // Simulazione di repository con utenti
        this.apiUserRepository = Map.of(
            "test", ApiUser.builder()
                .id(1435984560876356L)
                .accessId("test")
                .secret(BCrypt.hashpw("test", BCrypt.gensalt()))
                .role(RoleType.ROLE_API)
                .build()
        );
    }

    public Mono<AuthResponse> login(String accessId, String secret) {
        
        // 1. Cerca l'utente nel repository
        ApiUser apiUser = apiUserRepository.get(accessId);
        if (apiUser == null) {
            throw new AccessNotAuthorizedException();
        }

        // 2. Verifica la password con BCrypt
        if (!BCrypt.checkpw(secret, apiUser.getSecret())) {
            throw new AccessNotAuthorizedException();
        }

        // 3. Genera JWT token
        String[] roles = {apiUser.getRole()};
        Long expirationSeconds = 300L;  // 5 minuti
        Instant now = Instant.now();

        Map<String, Object> claims = new HashMap<>();
        claims.put("jti", UUID.randomUUID().toString());
        claims.put("id", apiUser.getId());
        claims.put("sub", accessId);           // Subject (username)
        claims.put("iss", "demo");             // Issuer
        claims.put("roles", roles);            // Ruoli
        claims.put("iat", Date.from(now));     // Issued At
        claims.put("exp", Date.from(now.plus(expirationSeconds, ChronoUnit.SECONDS))); // Expiration

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
```

**Flusso di login:**

1. **Ricerca utente**: Verifica che l'utente esista nel sistema
2. **Verifica password**: Usa BCrypt per comparare la password con l'hash memorizzato
3. **Generazione JWT**: Crea un token con claims e firma
4. **Scadenza**: Token valido per 5 minuti (configurabile)

### Hashing della Password con BCrypt

Non memorizzare mai le password in chiaro!

**Hashing al momento della creazione dell'utente:**
```java
String plainPassword = "mySecurePassword123";
String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
// Store hashedPassword in database
```

**Verifica della password al login:**
```java
String plaintextPassword = authRequest.getSecret();
String storedHash = apiUser.getSecret();

boolean passwordMatch = BCrypt.checkpw(plaintextPassword, storedHash);
```

## 5. Controller con Autenticazione

```java
@Controller("secure")
@ExecuteOn(TaskExecutors.IO)
public class AuthController {

    @Inject
    AuthService authService;

    // Endpoint di login (pubblico)
    @Post("login")
    @Secured({RoleType.IS_ANONYMOUS})
    public Mono<AuthResponse> login(@Body @Valid AuthRequest authRequest) {
        return authService.login(authRequest.getAccessId(), authRequest.getSecret());
    }

    // Endpoint protetto (richiede autenticazione)
    @Get("authInfo")
    @Secured({RoleType.ROLE_API})
    public Authentication info(Authentication authentication) {
        return authentication;
    }
}
```

**Annotazioni di sicurezza:**
- `@Secured({RoleType.IS_ANONYMOUS})`: Endpoint pubblico, nessun token necessario
- `@Secured({RoleType.ROLE_API})`: Richiede token con ruolo ROLE_API
- `Authentication authentication`: Inietta i dati dell'utente autenticato

## 6. Utilizzo Negli Endpoint

### Endpoint Pubblico

```java
@Post("login")
@Secured({RoleType.IS_ANONYMOUS})
public Mono<AuthResponse> login(@Body @Valid AuthRequest authRequest) {
    return authService.login(authRequest.getAccessId(), authRequest.getSecret());
}
```

**Utilizzo:**
```bash
curl -X POST http://localhost:8080/secure/login \
  -H "Content-Type: application/json" \
  -d '{"accessId": "test", "secret": "test"}'

# Risposta:
# {
#   "username": "test",
#   "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
#   "token_type": "Bearer",
#   "expires_in": 300,
#   "roles": ["ROLE_API"]
# }
```

### Endpoint Protetto

```java
@Get("authInfo")
@Secured({RoleType.ROLE_API})
public Authentication info(Authentication authentication) {
    return authentication;
}
```

**Utilizzo:**
```bash
curl -X GET http://localhost:8080/secure/authInfo \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Risposta:
# {
#   "name": "test",
#   "roles": ["ROLE_API"],
#   ...
# }
```

### Endpoint con Autorizzazione Multipla

```java
@Post("admin/delete/{id}")
@Secured({RoleType.ROLE_ADMIN, RoleType.ROLE_MANAGER})
public void deleteUser(Long id) {
    // Solo admin o manager possono accedere
}
```

### Usare i Dati dell'Utente Autenticato

```java
@Get("user/profile")
@Secured({RoleType.IS_AUTHENTICATED})
public UserProfile getUserProfile(Authentication authentication) {
    String username = (String) authentication.getName();
    String role = authentication.getRoles().iterator().next();
    
    return userService.getProfile(username);
}
```

## 7. Flusso Completo di Autenticazione

### Step-by-Step

```
1. Client invia credenziali
   POST /secure/login
   {"accessId": "test", "secret": "test"}

2. AuthService verifica credenziali
   - Cerca utente nel repository
   - Usa BCrypt.checkpw() per verificare password

3. Se valide, genera JWT
   - Crea claims (id, username, ruoli, scadenza)
   - Firma il token con la chiave segreta

4. Ritorna token al client
   HTTP 200 OK
   {
     "access_token": "eyJhbGc...",
     "expires_in": 300,
     "roles": ["ROLE_API"]
   }

5. Client memorizza il token
   (localStorage, sessionStorage, cookie)

6. Client invia token per richiedere risorsa protetta
   GET /secure/authInfo
   Authorization: Bearer eyJhbGc...

7. Micronaut verifica il token
   - Verifica la firma
   - Verifica la scadenza
   - Estrae i ruoli

8. Micronaut verifica i ruoli
   - @Secured({RoleType.ROLE_API})
   - Controlla se il token contiene ROLE_API

9. Se autorizzato, esegue il metodo
   Altrimenti ritorna 401 o 403
```

## 8. Configurazione

### application.yml - Configurazione Completa di Sicurezza

```yaml
micronaut:
  security:
    # Tipo di autenticazione: bearer token (JWT)
    authentication: bearer
    
    # Disabilita gli endpoint default di Micronaut
    endpoints:
      login:
        enabled: false          # Usa AuthController custom
      oauth:
        enabled: false          # Non usiamo OAuth
    
    # Configurazione JWT Token
    token:
      jwt:
        signatures:
          secret:
            generator:
              # Chiave segreta per firmare i JWT
              # In produzione: usa variabile d'ambiente!
              secret: ${JWT_GENERATOR_SIGNATURE_SECRET:pleaseChangeThisSecretForANewOne}
    
    # Mapping degli URL per controllo d'accesso
    interceptUrlMap:
      # Endpoint pubblici (demo)
      - pattern: /demo/**
        access:
          - isAnonymous()      # Pubblico, nessun token
      
      # Swagger e documentazione API (pubblico)
      - pattern: /swagger/**
        httpMethod: GET
        access:
          - isAnonymous()
      
      - pattern: /swagger-ui/**
        httpMethod: GET
        access:
          - isAnonymous()
      
      # Tutti gli altri endpoint richiedono autenticazione
      - pattern: /**
        access:
          - isAuthenticated()  # Richiede token valido
```

**Configurazione spiegata:**

| Parametro | Valore | Significato |
|-----------|--------|-------------|
| `authentication` | `bearer` | Usa JWT Bearer token authentication |
| `endpoints.login.enabled` | `false` | Usa il custom AuthController |
| `endpoints.oauth.enabled` | `false` | OAuth non abilitato |
| `token.jwt.signatures.secret.generator.secret` | Environment var | Chiave per firmare JWT |
| `interceptUrlMap` | URL patterns | Definisce accesso pubblico/protetto |

### Variabili d'Ambiente

Imposta la chiave segreta tramite variabile d'ambiente:

```bash
# Development
export JWT_GENERATOR_SIGNATURE_SECRET="dev-secret-change-in-production"

# Production (chiave forte almeno 32 caratteri)
export JWT_GENERATOR_SIGNATURE_SECRET="$(openssl rand -base64 32)"
```

### Aggiungere Nuovi Endpoint Pubblici

Se vuoi aggiungere un nuovo endpoint pubblico (es. `/health`):

```yaml
micronaut:
  security:
    interceptUrlMap:
      - pattern: /health/**
        access:
          - isAnonymous()
      
      - pattern: /demo/**
        access:
          - isAnonymous()
      
      - pattern: /secure/login
        access:
          - isAnonymous()      # Login deve essere pubblico!
      
      # ... altri pattern
```

### Proteggere Specifici HTTP Methods

```yaml
micronaut:
  security:
    interceptUrlMap:
      # GET pubblico, POST protetto
      - pattern: /products
        httpMethod: GET
        access:
          - isAnonymous()
      
      - pattern: /products
        httpMethod: POST
        access:
          - hasRole('ROLE_ADMIN')
```

### Pattern Matching

I pattern supportano:
- `/**`: Tutti gli endpoint
- `/secure/**`: Tutti sotto `/secure`
- `/secure/profile`: Esatto
- Regex con `^` e `$`

## 9. Best Practices

### 1. Non Memorizzare Password in Chiaro

```java
// ❌ CATTIVO: Mai fare questo
user.setPassword("plainTextPassword");

// ✅ BUONO: Usa BCrypt
user.setPassword(BCrypt.hashpw(plainPassword, BCrypt.gensalt()));
```

### 2. Usa HTTPS in Produzione

I JWT sono firmati ma non criptati. Usa sempre HTTPS per trasmettere token.

### 3. Implementa Refresh Token

```java
// TTL breve per access token (5-15 minuti)
// TTL lungo per refresh token (giorni/settimane)
@Data
@Builder
public class RefreshTokenResponse {
    String access_token;
    Long expires_in;
    String refresh_token;
}
```

### 4. Valida il Token Alla Ricezione

```java
// Micronaut lo fa automaticamente con @Secured
// Ma puoi aggiungere validazione custom:
if (authentication == null) {
    throw new UnauthorizedException();
}
```

### 5. Implementa Token Revocation

```java
@Singleton
public class TokenBlacklistService {
    private Set<String> blacklist = ConcurrentHashMap.newKeySet();
    
    public void revoke(String token) {
        blacklist.add(token);
    }
    
    public boolean isRevoked(String token) {
        return blacklist.contains(token);
    }
}
```

### 6. Logging di Sicurezza

```java
@Singleton
public class AuthService {
    
    private final Log log = LoggerFactory.getLogger(AuthService.class);
    
    public Mono<AuthResponse> login(String accessId, String secret) {
        try {
            // ... validazione
            log.info("Successful login for user: {}", accessId);
            return buildResponse(apiUser);
        } catch (AccessNotAuthorizedException e) {
            log.warn("Failed login attempt for user: {}", accessId);
            throw e;
        }
    }
}
```

### 7. Protezione contro Brute Force

```java
@Singleton
public class BruteForceProtection {
    private Map<String, Integer> failedAttempts = new ConcurrentHashMap<>();
    
    public void recordFailedLogin(String accessId) {
        int attempts = failedAttempts.getOrDefault(accessId, 0) + 1;
        if (attempts > 5) {
            throw new AccountLockedException();
        }
        failedAttempts.put(accessId, attempts);
    }
}
```

### 8. Scadenza Appropriata del Token

```java
// ✅ BUONO: Token a breve termine
Long expirationSeconds = 300L;  // 5 minuti

// ❌ CATTIVO: Token non scade mai
Long expirationSeconds = Long.MAX_VALUE;
```

## 10. Autorizzazione Basata Su Ruoli

### Ruoli Singoli

```java
@Get("user/profile")
@Secured({RoleType.ROLE_API})
public UserProfile getUserProfile() { }
```

### Ruoli Multipli (OR)

```java
@Delete("user/{id}")
@Secured({RoleType.ROLE_ADMIN, RoleType.ROLE_MANAGER})
public void deleteUser(Long id) {
    // Sia admin che manager possono accedere
}
```

### Autorizzazione Granulare

```java
@Post("resource/create")
@Secured({RoleType.IS_AUTHENTICATED})
public void createResource(@Body Resource resource, Authentication auth) {
    // Verifica aggiuntiva: solo il proprietario può creare
    if (!isResourceOwner(resource, auth)) {
        throw new ForbiddenException();
    }
    resourceService.save(resource);
}
```

## 11. Test di Sicurezza

```java
@MicronautTest
class AuthControllerTest {
    
    @Client
    HttpClient client;
    
    @Test
    void testLoginSuccess() {
        AuthRequest request = new AuthRequest();
        request.setAccessId("test");
        request.setSecret("test");
        
        AuthResponse response = client.toBlocking()
            .retrieve(POST, "/secure/login", request, AuthResponse.class);
        
        assertNotNull(response.getAccess_token());
        assertTrue(response.getRoles().contains(RoleType.ROLE_API));
    }
    
    @Test
    void testLoginFailure() {
        AuthRequest request = new AuthRequest();
        request.setAccessId("test");
        request.setSecret("wrong_password");
        
        assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().retrieve(POST, "/secure/login", request);
        });
    }
    
    @Test
    void testProtectedEndpointWithoutToken() {
        assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().retrieve("/secure/authInfo");
        });
    }
    
    @Test
    void testProtectedEndpointWithToken() {
        String token = obtainToken();
        
        HttpRequest<?> request = HttpRequest.GET("/secure/authInfo")
            .bearerAuth(token);
        
        Authentication auth = client.toBlocking()
            .retrieve(request, Authentication.class);
        
        assertNotNull(auth);
    }
}
```

## 12. Integrazione con il Codice Reale

### Sostituire il Repository Mock

**Attualmente (mock):**
```java
this.apiUserRepository = Map.of(
    "test", ApiUser.builder()...
);
```

**In produzione (con database):**
```java
@Singleton
public class AuthService {
    
    private final UserRepository userRepository;
    private final JwtTokenGenerator tokenGenerator;
    
    public AuthService(UserRepository userRepository, JwtTokenGenerator tokenGenerator) {
        this.userRepository = userRepository;
        this.tokenGenerator = tokenGenerator;
    }
    
    public Mono<AuthResponse> login(String accessId, String secret) {
        ApiUser apiUser = userRepository.findByAccessId(accessId)
            .orElseThrow(AccessNotAuthorizedException::new);
        // ...
    }
}
```

## Integrazione con il Framework

Consulta:
- [Error Handling](../exceptions/README.md) per gestire `AccessNotAuthorizedException`
- [CRUD Example](../crud/README.md) per proteggere operazioni CRUD
- [ARCHITECTURE.md](../ARCHITECTURE.md) per architettura generale
- [CODING_GUIDELINES.md](../CODING_GUIDELINES.md) per linee guida di codifica

## Risorse Esterne

- [Micronaut Security Documentation](https://micronaut-projects.github.io/micronaut-security/latest)
- [JWT Introduction](https://jwt.io)
- [BCrypt Password Hashing](https://www.mindrot.org/projects/jbcrypt/)
