# Internal Request Example - Calling External Microservices with Authentication

Questo esempio dimostra come fare richieste HTTP a microservizi esterni (o interni in un'architettura distribuita) con autenticazione tramite Google Cloud Credentials, utilizzando identity tokens.

## Concetti Fondamentali

### Comunicazione tra Microservizi

In un'architettura distribuita con microservizi, spesso è necessario:
- Chiamare endpoint di altri servizi
- Autenticarsi per accedere a servizi protetti
- Gestire errori di comunicazione
- Implementare retry e timeout

### Google Cloud Identity Tokens

Google Cloud fornisce:
- **Access Token**: Per accedere alle API Google Cloud
- **Identity Token**: Per accedere ad altri servizi Cloud Run/Kubernetes in modo sicuro
- **Service Account**: Identità del servizio per autenticazione

```
┌──────────────────────┐
│ Microservizio A      │
│ (Questo servizio)    │
└──────────┬───────────┘
           │ 1. Richiede token a Google Cloud
           ▼
┌──────────────────────────────────┐
│ Google Cloud Credential Provider │
│ (IdTokenProvider)                │
└──────────┬───────────────────────┘
           │ 2. Ritorna Identity Token
           ▼
┌──────────────────────┐
│ Microservizio B      │
│ (Target endpoint)    │
│ - Verifica token     │
│ - Esegue richiesta   │
└──────────────────────┘
```

## 1. Configurazione di Google Cloud Credentials

### Prerequisiti

1. **Google Cloud Project** con servizi abilitati
2. **Service Account** con permessi appropriati
3. **GOOGLE_APPLICATION_CREDENTIALS** - File JSON con credenziali

### Impostare le Credenziali

```bash
# Scarica le credenziali della Service Account da Google Cloud Console
# Salva il file JSON, es: service-account-key.json

# Imposta la variabile d'ambiente
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key.json
```

### Iniettare GoogleCredentials in Micronaut

```java
@Singleton
public class CredentialsProvider {
    
    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        return GoogleCredentials.getApplicationDefault();
    }
}
```

Micronaut caricherà automaticamente le credenziali da:
1. `GOOGLE_APPLICATION_CREDENTIALS` env variable
2. Default Application Credentials
3. Metadata server di Google Cloud (se su Google Cloud Platform)

## 2. InternalRequestController - Richieste a Microservizi

```java
@Singleton
public class InternalRequestController {

    private final GoogleCredentials googleCredentials;
    private final HttpClient httpClient;

    public InternalRequestController(GoogleCredentials googleCredentials, HttpClient httpClient) {
        this.googleCredentials = googleCredentials;
        this.httpClient = httpClient;
    }

    @ExecuteOn(TaskExecutors.BLOCKING)
    public String intRequest() throws IOException {
        // 1. Definisci endpoint target
        String endpoint = "https://contentprovider-dummy-42386345137.europe-west1.run.app/health";
        
        // 2. Ottieni token di accesso
        String accessToken = this.getAccessTokenCredentials(endpoint);

        // 3. Costruisci richiesta HTTP con token
        HttpRequest<?> request = HttpRequest.GET(endpoint)
            .bearerAuth(accessToken)
            .accept(MediaType.APPLICATION_JSON);

        // 4. Esegui richiesta e gestisci errori
        try {
            return httpClient.toBlocking().retrieve(request, String.class);
        } catch (HttpClientResponseException e) {
            System.err.println("Errore HTTP: " + e.getStatus() + " - " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            throw e;
        }
    }

    // Ottiene il token Identity per l'endpoint target
    private String getAccessTokenCredentials(String targetAudience) throws IOException {
        
        // Verifica che le credenziali implementino IdTokenProvider
        if (!(googleCredentials instanceof IdTokenProvider)) {
            throw new RuntimeException("Credentials are not an instance of IdTokenProvider.");
        }
        
        // Crea Identity Token credentials
        IdTokenCredentials idTokenCredentials = IdTokenCredentials.newBuilder()
            .setIdTokenProvider((IdTokenProvider) googleCredentials)
            .setTargetAudience(targetAudience.replaceAll("/+$", ""))
            .build();
        
        // Ritorna il token
        return idTokenCredentials.refreshAccessToken().getTokenValue();
    }
}
```

## 3. Componenti Principali

### GoogleCredentials

```java
private final GoogleCredentials googleCredentials;
```

- Fornite da Google Cloud
- Rappresentano l'identità del servizio
- Permettono di ottenere token Identity

### HttpClient

```java
private final HttpClient httpClient;
```

- Client HTTP di Micronaut
- Non-blocking per le operazioni
- Integrato nel framework

### HttpRequest Builder

```java
HttpRequest<?> request = HttpRequest.GET(endpoint)
    .bearerAuth(accessToken)
    .accept(MediaType.APPLICATION_JSON);
```

**Metodi disponibili:**
- `.GET(url)`, `.POST(url)`, `.PUT(url)`, `.DELETE(url)`
- `.bearerAuth(token)`: Aggiunge header `Authorization: Bearer <token>`
- `.accept(MediaType)`: Aggiunge header `Accept`
- `.header(name, value)`: Header personalizzato
- `.body(content)`: Body della richiesta

## 4. Flusso Completo

### Step-by-Step

```
1. Client chiama endpoint locale
   GET /local/endpoint

2. Microservizio A riceve la richiesta
   InternalRequestController.intRequest()

3. Richiedi token Identity a Google Cloud
   IdTokenCredentials.newBuilder()
   .setIdTokenProvider(googleCredentials)
   .setTargetAudience("https://servizio-target.com")

4. Costruisci richiesta HTTP al servizio target
   HttpRequest.GET(endpoint).bearerAuth(accessToken)

5. Esegui richiesta HTTP
   httpClient.toBlocking().retrieve(request)

6. Servizio target riceve richiesta con token
   - Valida il token
   - Verifica permessi
   - Esegue l'operazione

7. Ritorna risposta
   HttpResponse con risultato

8. Gestisci eventuali errori
   HttpClientResponseException

9. Ritorna risposta al client originale
   HTTP Response
```

## 5. Utilizzo Pratico

### Chiamare un Endpoint Semplice (GET)

```java
@Singleton
public class WeatherServiceClient {
    
    private final GoogleCredentials googleCredentials;
    private final HttpClient httpClient;
    
    public String getWeather(String city) throws IOException {
        String endpoint = "https://weather-service.run.app/weather?city=" + city;
        String token = getAccessTokenCredentials(endpoint);
        
        HttpRequest<?> request = HttpRequest.GET(endpoint)
            .bearerAuth(token);
        
        return httpClient.toBlocking().retrieve(request, String.class);
    }
}
```

### Inviare Dati (POST)

```java
@Singleton
public class OrderServiceClient {
    
    private final GoogleCredentials googleCredentials;
    private final HttpClient httpClient;
    
    public OrderResponse createOrder(OrderRequest order) throws IOException {
        String endpoint = "https://order-service.run.app/orders";
        String token = getAccessTokenCredentials(endpoint);
        
        HttpRequest<?> request = HttpRequest.POST(endpoint, order)
            .bearerAuth(token)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON);
        
        return httpClient.toBlocking().retrieve(request, OrderResponse.class);
    }
}
```

### Con Timeout

```java
HttpRequest<?> request = HttpRequest.GET(endpoint)
    .bearerAuth(token)
    .accept(MediaType.APPLICATION_JSON);

Duration timeout = Duration.ofSeconds(30);
return httpClient.toBlocking().retrieve(request, String.class);
```

### Con Query Parameters

```java
String endpoint = "https://api.example.com/search";
String query = "SELECT * WHERE status=active";

UriBuilder.of(endpoint)
    .queryParam("q", query)
    .queryParam("limit", 10)
    .build();

HttpRequest<?> request = HttpRequest.GET(url)
    .bearerAuth(token);
```

## 6. Gestione degli Errori

### Errori HTTP

```java
try {
    return httpClient.toBlocking().retrieve(request, String.class);
} catch (HttpClientResponseException e) {
    int status = e.getStatus().getCode();
    String body = e.getResponse().getBody(String.class).orElse("");
    
    switch (status) {
        case 401:
            throw new UnauthorizedException("Invalid token");
        case 403:
            throw new ForbiddenException("Access denied");
        case 404:
            throw new ResourceNotFoundException("Endpoint not found");
        case 500:
            throw new InternalServerException("Target service error");
        default:
            throw new CommunicationException("HTTP " + status);
    }
} catch (ConnectException e) {
    throw new ServiceUnavailableException("Cannot reach service: " + endpoint);
} catch (TimeoutException e) {
    throw new RequestTimeoutException("Service took too long to respond");
}
```

### Retry con Backoff Esponenziale

```java
@Singleton
public class ResilientHttpClient {
    
    private final HttpClient httpClient;
    private static final int MAX_RETRIES = 3;
    
    public <T> T retrieve(HttpRequest<?> request, Class<T> responseClass) {
        int attempt = 0;
        long waitTime = 100; // ms
        
        while (attempt < MAX_RETRIES) {
            try {
                return httpClient.toBlocking().retrieve(request, responseClass);
            } catch (HttpClientResponseException e) {
                if (e.getStatus().getCode() >= 500) {
                    attempt++;
                    if (attempt < MAX_RETRIES) {
                        try {
                            Thread.sleep(waitTime);
                            waitTime *= 2; // Backoff esponenziale
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(ie);
                        }
                        continue;
                    }
                }
                throw e;
            }
        }
        throw new RuntimeException("Max retries reached");
    }
}
```

## 7. Identity Token vs Access Token

| Aspetto | Identity Token | Access Token |
|--------|---|---|
| **Scopo** | Autenticare utente/servizio | Accedere alle API Google |
| **Audience** | Servizio specifico | Google Cloud APIs |
| **TTL** | 1 ora | 1 ora |
| **Claims** | email, sub, aud, iat, exp | scopes, aud, iat, exp |
| **Uso** | Service-to-Service Auth | Google Cloud Services |

```java
// Identity Token - Per servizio specifico
IdTokenCredentials idToken = IdTokenCredentials.newBuilder()
    .setIdTokenProvider((IdTokenProvider) googleCredentials)
    .setTargetAudience("https://my-service.run.app")
    .build();

// Access Token - Per Google Cloud APIs
AccessToken accessToken = googleCredentials.refreshAccessToken();
```

## 8. Architetture Supportate

### Google Cloud Run

```java
// Chiamare un altro Cloud Run service
String endpoint = "https://my-service-xxxxx.run.app/api/endpoint";
```

### Google Kubernetes Engine (GKE)

```java
// Interno al cluster usando internal DNS
String endpoint = "http://my-service:8080/api/endpoint";

// O tramite Cloud Run per visibilità esterna
String endpoint = "https://my-service-xxxxx.run.app/api/endpoint";
```

### Hybrid (On-premises + Cloud)

```java
// Endpoint ibrido con autenticazione Google
String endpoint = "https://my-onprem-service.example.com:443/api";
String token = getAccessTokenCredentials(endpoint);
```

## 9. Best Practices

### 1. Cache dei Token

```java
@Singleton
public class TokenCache {
    
    private Map<String, CachedToken> cache = new ConcurrentHashMap<>();
    
    public String getToken(String audience, IdTokenProvider provider) throws IOException {
        CachedToken cached = cache.get(audience);
        
        // Refresh se scaduto (5 minuti prima della scadenza)
        if (cached == null || cached.isExpiredSoon()) {
            IdTokenCredentials credentials = IdTokenCredentials.newBuilder()
                .setIdTokenProvider(provider)
                .setTargetAudience(audience)
                .build();
            
            AccessToken token = credentials.refreshAccessToken();
            cached = new CachedToken(token.getTokenValue(), token.getExpirationTime());
            cache.put(audience, cached);
        }
        
        return cached.getToken();
    }
}
```

### 2. Gestione dell'Audience

```java
// Sempre normalizzare l'audience (rimuovere trailing slash)
String audience = targetUrl.replaceAll("/+$", "");

// Verificare che sia un URL valido
if (!audience.startsWith("http://") && !audience.startsWith("https://")) {
    throw new IllegalArgumentException("Invalid audience: " + audience);
}
```

### 3. Logging di Sicurezza

```java
@Singleton
public class SecureHttpClient {
    
    private static final Logger log = LoggerFactory.getLogger(SecureHttpClient.class);
    
    public <T> T retrieve(HttpRequest<?> request, Class<T> responseClass) {
        try {
            log.info("Calling endpoint: {} {}", request.getMethod(), request.getUri());
            return httpClient.toBlocking().retrieve(request, responseClass);
        } catch (HttpClientResponseException e) {
            log.error("HTTP error: {} {} - {}", 
                request.getMethod(), request.getUri(), e.getStatus());
            throw e;
        } catch (Exception e) {
            log.error("Communication error with {}: {}", 
                request.getUri(), e.getMessage());
            throw e;
        }
    }
}
```

### 4. Non Esporre Dettagli di Implementazione

```java
// ❌ CATTIVO: Espone dettagli della infrastrutura
throw new Exception("Cannot reach https://internal-service.gke.company.com:443");

// ✅ BUONO: Messaggio generico
throw new ServiceUnavailableException("Service temporarily unavailable");
```

### 5. Circuit Breaker Pattern

```java
@Singleton
public class CircuitBreakerClient {
    
    private int failureCount = 0;
    private long lastFailureTime = 0;
    private static final int FAILURE_THRESHOLD = 5;
    private static final long TIMEOUT = 60000; // 1 minuto
    
    public <T> T retrieve(HttpRequest<?> request, Class<T> responseClass) {
        if (isCircuitOpen()) {
            throw new ServiceUnavailableException("Service circuit breaker is open");
        }
        
        try {
            T result = httpClient.toBlocking().retrieve(request, responseClass);
            failureCount = 0; // Reset su successo
            return result;
        } catch (Exception e) {
            failureCount++;
            lastFailureTime = System.currentTimeMillis();
            
            if (failureCount >= FAILURE_THRESHOLD) {
                throw new ServiceUnavailableException("Too many failures, circuit opened");
            }
            throw e;
        }
    }
    
    private boolean isCircuitOpen() {
        if (failureCount < FAILURE_THRESHOLD) {
            return false;
        }
        
        long timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime;
        if (timeSinceLastFailure > TIMEOUT) {
            failureCount = 0; // Reset dopo timeout
            return false;
        }
        
        return true;
    }
}
```

### 6. Timeout Appropriati

```java
// Timeout per operazioni diverse
private static final Duration SHORT_TIMEOUT = Duration.ofSeconds(5);    // Health check
private static final Duration NORMAL_TIMEOUT = Duration.ofSeconds(30);  // API normali
private static final Duration LONG_TIMEOUT = Duration.ofSeconds(60);    // Upload/Export
```

## 10. Configurazione nell'application.yml

```yaml
# HTTP Client configuration
micronaut:
  http:
    client:
      connect-timeout: 10s
      read-timeout: 30s
      max-content-length: 10MB
  
  # Google Cloud
  gcp:
    project-id: ${GCP_PROJECT_ID}
    credentials:
      location: ${GOOGLE_APPLICATION_CREDENTIALS:/path/to/creds}

# Servizi remoti (configuration esterna)
services:
  weather-service:
    url: ${WEATHER_SERVICE_URL:https://weather-service.run.app}
  order-service:
    url: ${ORDER_SERVICE_URL:https://order-service.run.app}
```

## 11. Testing

```java
@MicronautTest
class InternalRequestControllerTest {
    
    @MockBean
    HttpClient httpClient;
    
    @Inject
    InternalRequestController controller;
    
    @Test
    void testSuccessfulRequest() throws IOException {
        String response = "{\"status\": \"ok\"}";
        
        Mockito.when(httpClient.toBlocking().retrieve(
            Mockito.any(), 
            Mockito.eq(String.class)
        )).thenReturn(response);
        
        String result = controller.intRequest();
        assertEquals(response, result);
    }
    
    @Test
    void testHttpError() throws IOException {
        HttpClientResponseException exception = 
            Mockito.mock(HttpClientResponseException.class);
        
        Mockito.when(httpClient.toBlocking().retrieve(
            Mockito.any(), 
            Mockito.eq(String.class)
        )).thenThrow(exception);
        
        assertThrows(HttpClientResponseException.class, () -> {
            controller.intRequest();
        });
    }
}
```

## 12. Diagrammi di Sequenza

### Happy Path

```
Client          └─> Service A          └─> Service B       └─> Google Cloud
  │                  │                      │                    │
  ├─request─────────>│                      │                    │
  │                  ├─request token───────────────────────────>│
  │                  |<─────token────────────────────────────────┤
  │                  ├─GET + token─────────>│                    │
  │                  │<────response─────────┤                    │
  │<─response────────┤                      │                    │
```

### Error Case - Token Scaduto

```
Service A          └─> Google Cloud      └─> Service B
  │                    │                     │
  ├─request token─────>│                     │
  │<─expired token──────┤                     │
  │                                           │
  ├─new token request──>│                     │
  │<─new token──────────┤                     │
  │                                           │
  ├─GET + new token────────────────────────>│
  │<─────response──────────────────────────────┤
```

## Integrazione con il Framework

Consulta:
- [CRUD Example](../crud/README.md) per operazioni di base
- [Security Example](../security/README.md) per autenticazione e autorizzazione
- [Exception Handling](../exceptions/README.md) per gestire errori di comunicazione
- [ARCHITECTURE.md](../ARCHITECTURE.md) per architettura generale
- [CODING_GUIDELINES.md](../CODING_GUIDELINES.md) per linee guida di codifica

## Risorse Esterne

- [Google Cloud Identity Tokens](https://cloud.google.com/docs/authentication/token-types)
- [Micronaut HTTP Client](https://micronaut-projects.github.io/micronaut-core/latest/guide/#httpClient)
- [Google Cloud Java Client](https://github.com/googleapis/google-cloud-java)
