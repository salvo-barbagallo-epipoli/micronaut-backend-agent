# Exception Handling Example - Global Error Controller

Questo esempio dimostra come implementare una gestione centralizzata delle eccezioni (Global Exception Handler) in Micronaut, per fornire risposte HTTP coerenti e ben strutturate.

## Concetti Fondamentali

Un **Global Exception Handler** intercetta tutte le eccezioni lanciate nell'applicazione e le trasforma in risposte HTTP appropriate. Vantaggi:

- **Risposta coerente**: Tutti gli errori hanno lo stesso formato
- **Centralizzato**: Un unico punto di gestione
- **Separazione delle responsabilità**: Controller rimangono puliti
- **Logging e monitoring**: Facile tracciare errori
- **API ben documentata**: Errori prevedibili per il client

## Architettura

```
┌────────────────────────────────┐
│      Controller / Service      │
│   (Lanciano eccezioni)         │
└────────────┬────────────────────┘
             │
             ▼
┌────────────────────────────────┐
│   Eccezione lanciata           │
└────────────┬────────────────────┘
             │
             ▼
┌────────────────────────────────┐
│   ErrorController              │
│   (Global Exception Handler)   │
│   - Intercetta exception       │
│   - Mappa a HttpResponse       │
│   - Formatta ErrorMessage      │
└────────────┬────────────────────┘
             │
             ▼
┌────────────────────────────────┐
│   HTTP Response                │
│   {errorCode, message}         │
└────────────────────────────────┘
```

## 1. Exception Base - DemoException

Crea una classe base per tutte le eccezioni personalizzate:

```java
@Getter
public class DemoException extends RuntimeException {
    private int code;        // Codice errore personalizzato
    private int httpStatus;  // HTTP Status Code

    public DemoException(int code, int httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }
}
```

**Caratteristiche:**
- Estende `RuntimeException` (eccezione non controllata)
- Contiene codice errore personalizzato per logging/monitoring
- Contiene HTTP status per la risposta
- Message è il messaggio di errore

## 2. Exception Specifiche

Crea eccezioni specifiche che estendono `DemoException`:

```java
@Getter
public class ProductNotFoundException extends DemoException {
    public ProductNotFoundException() {
        super(404005, 404, "Product Not Found");
    }
}
```

```java
@Getter
public class RequestAlreadyInProgressException extends DemoException {
    public RequestAlreadyInProgressException() {
        super(429001, 419, "Request already in progress");
    }
}
```

```java
@Getter
public class AccessNotAuthorizedException extends DemoException {
    public AccessNotAuthorizedException() {
        super(401000, 401, "Not authorized");
    }
}
```

```java
@Getter
public class CustomerNotFoundException extends DemoException {
    public CustomerNotFoundException() {
        super(404001, 404, "Customer Not Found");
    }
}
```

**Convenzione di codici errore:**
- `4XX0XX`: Errori client (4 prefisso HTTP)
- `5XX0XX`: Errori server (5 prefisso HTTP)
- `XXY00Z`: Y = numero errore specifico, Z = variante

## 3. Error Message DTO

Crea il DTO per la risposta di errore:

```java
@Getter
@Serdeable
@ReflectiveAccess
public class ErrorMessage {
    private Integer errorCode;
    private String message;

    public ErrorMessage(Integer errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }
}
```

**Nella risposta HTTP:**
```json
{
  "errorCode": 404005,
  "message": "Product Not Found"
}
```

## 4. Global Exception Handler - ErrorController

Il controller con `@Error(global = true)` intercetta tutte le eccezioni:

```java
@Controller
public class ErrorController {

    @Error(global = true)
    public HttpResponse<ErrorMessage> handleDemoException(
        HttpRequest<?> request, 
        DemoException exception
    ) {
        return HttpResponse.status(HttpStatus.valueOf(exception.getHttpStatus()))
                .body(new ErrorMessage(
                        exception.getCode(),
                        exception.getMessage()
                ));
    }
}
```

**Come funziona:**
1. `@Error(global = true)`: Intercetta eccezioni a livello globale
2. Seleziona quale metodo usare in base al tipo di eccezione (polimorfismo)
3. Restituisce `HttpResponse<ErrorMessage>` con status e body

### Gestione di Eccezioni Personalizzate

```java
@Error(global = true)
public HttpResponse<ErrorMessage> handleDemoException(
    HttpRequest<?> request, 
    DemoException exception
) {
    return HttpResponse.status(HttpStatus.valueOf(exception.getHttpStatus()))
            .body(new ErrorMessage(
                    exception.getCode(),
                    exception.getMessage()
            ));
}
```

### Gestione di Eccezioni Standard di Micronaut

```java
// Violazione di vincoli di validazione
@Error(global = true)
public HttpResponse<ErrorMessage> handleConstraintViolationException(
    HttpRequest<?> request, 
    ConstraintViolationException exception
) {
    return HttpResponse.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorMessage(
                    HttpStatus.BAD_REQUEST.getCode(),
                    exception.getMessage()
            ));
}

// Errori di conversione parametri
@Error(global = true)
public HttpResponse<ErrorMessage> handleConversionError(
    HttpRequest<?> request, 
    ConversionErrorException ex
) {
    return HttpResponse.badRequest(new ErrorMessage(4000000, ex.getMessage()));
}

// Autorizzazione non valida
@Error(global = true)
public HttpResponse<ErrorMessage> handleAuthorizationException(
    HttpRequest<?> request, 
    AuthorizationException exception
) {
    return HttpResponse.status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorMessage(
                    HttpStatus.UNAUTHORIZED.getCode(),
                    exception.getMessage()
            ));
}

// Errori del framework HTTP
@Error(global = true)
public HttpResponse<ErrorMessage> handleHttpClientError(
    HttpRequest<?> request, 
    HttpClientResponseException ex
) {
    return HttpResponse.badRequest(new ErrorMessage(4000000, ex.getMessage()));
}

// Eccezioni generiche
@Error(global = true)
public HttpResponse<ErrorMessage> handleIllegalArgument(
    HttpRequest<?> request, 
    IllegalArgumentException ex
) {
    return HttpResponse.badRequest(new ErrorMessage(4000000, ex.getMessage()));
}
```

### Eccezioni Framework Specifiche

```java
// Eccezioni specifiche del framework Highways
@Error(global = true)
public HttpResponse<ErrorMessage> handleUniqueException(
    HttpRequest<?> request, 
    HighwaysUniqueException exception
) {
    return HttpResponse.status(HttpStatus.CONFLICT)
            .body(new ErrorMessage(
                    HttpStatus.CONFLICT.getCode(),
                    exception.getMessage()
            ));
}

// Errori interni del server
@Error(global = true)
public HttpResponse<ErrorMessage> handleInternalServer(
    HttpRequest<?> request, 
    InternalServerException ex
) {
    return HttpResponse.badRequest(new ErrorMessage(5000000, ex.getMessage()));
}
```

## Utilizzo Nelle Applicazioni

### Nel Service

```java
@Singleton
public class ProductService {
    
    private final HwEntityService hwEntityService;

    public Product productDetail(String productSku) {
        // Lancia eccezione se non trovato
        return hwEntityService.findById(Product.class, productSku)
            .orElseThrow(ProductNotFoundException::new);
    }

    public void updateProduct(String productSku, UpdateProductRequest product) {
        // Lancia eccezione se non trovato
        hwEntityService.findById(Product.class, productSku)
            .orElseThrow(ProductNotFoundException::new);
        
        product.setId(productSku);
        hwEntityService.upsert(product);
    }
}
```

### Nel Controller

Non è necessario aggiungere try-catch! L'ErrorController gestisce tutto:

```java
@Controller("demo/product")
@ExecuteOn(TaskExecutors.BLOCKING)
public class ProductController {

    private final ProductService productService;

    @Get("{productSku}")
    public Product details(String productSku) {
        // Se non esiste, ProductNotFoundException viene lanciata
        // ErrorController la cattura e restituisce risposta 404
        return productService.productDetail(productSku);
    }

    @Put("{productSku}")
    public void update(String productSku, @Valid @Body UpdateProductRequest product) {
        // Stessa cosa per update
        productService.updateProduct(productSku, product);
    }
}
```

## Flusso di Gestione degli Errori

### Caso 1: Risorsa Non Trovata

```
1. Client: GET /demo/product/INVALID_SKU
2. Controller: productService.productDetail("INVALID_SKU")
3. Service: hwEntityService.findById(...).orElseThrow(ProductNotFoundException::new)
4. Exception: ProductNotFoundException lanciata (code 404005, httpStatus 404)
5. ErrorController: handleDemoException() cattura l'eccezione
6. Response: 404 {errorCode: 404005, message: "Product Not Found"}
```

### Caso 2: Validazione Fallita

```
1. Client: POST /demo/product {sku: "123", name: ""}
2. Micronaut: @Valid fallisce, ConstraintViolationException
3. ErrorController: handleConstraintViolationException() cattura
4. Response: 400 {errorCode: 400, message: "size must be between 2 and 40"}
```

### Caso 3: Autorizzazione Negata

```
1. Client: GET /admin/users (senza token)
2. Micronaut Security: AuthorizationException
3. ErrorController: handleAuthorizationException() cattura
4. Response: 401 {errorCode: 401, message: "Not authorized"}
```

## Codici di Errore - Convenzione

Suggerita: `PREFIX + CATEGORY + VARIANT`

```
┌─────────────────────────┐
│  Codice |  Significato  │
├─────────────────────────┤
│ 404005  │ Product not found    │
│ 404001  │ Customer not found   │
│ 401000  │ Not authorized       │
│ 409000  │ Conflict/duplicate   │
│ 419001  │ Already in progress  │
│ 400000  │ Bad request (generic)│
│ 500000  │ Internal error       │
└─────────────────────────┘
```

## Best Practices

### 1. Crea Eccezioni Semantiche

```java
// ✅ Buono: Eccezione specifica
throw new ProductNotFoundException();

// ❌ Cattivo: Eccezione generica
throw new Exception("Product not found");
```

### 2. Non Loggare Due Volte

```java
// ❌ Cattivo: Log sia nel service che nel controller
Service:
    catch (Exception e) {
        log.error("Product not found", e);
        throw new ProductNotFoundException();
    }

// ✅ Buono: Log solo nel Global Handler o nel service
```

### 3. Includi Dettagli Utili Nel Messaggio

```java
// ✅ Buono: Messaggio descrittivo
throw new ProductNotFoundException("SKU: INVALID_123 not found in region EU");

// ❌ Cattivo: Messaggio generico
throw new ProductNotFoundException();
```

### 4. Usa HTTP Status Appropriati

```java
// 404 Not Found - risorsa non esiste
// 400 Bad Request - input invalid
// 409 Conflict - violazione di vincoli unici
// 401 Unauthorized - autenticazione fallita
// 403 Forbidden - autenticazione OK, ma autorizzazione fallita
// 419 I'm a teapot - custom status per casi speciali
// 500 Internal Server Error - errore non gestito
```

### 5. Non Esporre Dettagli Interni

```java
// ❌ Cattivo: Espone dettagli di implementazione
message = "NullPointerException in ProductRepository.query()"

// ✅ Buono: Messaggio user-friendly
message = "Unable to retrieve product details"
```

### 6. Logging Centralizzato

Aggiungi logging nel Global Handler:

```java
@Error(global = true)
public HttpResponse<ErrorMessage> handleDemoException(
    HttpRequest<?> request, 
    DemoException exception
) {
    if (exception.getHttpStatus() >= 500) {
        log.error("Server error: {} - {} - {}", 
            exception.getCode(),
            exception.getMessage(),
            request.getPath()
        );
    } else {
        log.debug("Client error: {} - {} - {}", 
            exception.getCode(),
            exception.getMessage(),
            request.getPath()
        );
    }
    
    return HttpResponse.status(HttpStatus.valueOf(exception.getHttpStatus()))
            .body(new ErrorMessage(
                    exception.getCode(),
                    exception.getMessage()
            ));
}
```

## Estensioni Avanzate

### 1. Stack Trace in Sviluppo

```java
@Error(global = true)
public HttpResponse<?> handleDemoException(
    HttpRequest<?> request, 
    DemoException exception,
    @Property(name = "micronaut.environments") Collection<String> environments
) {
    ErrorMessage message = new ErrorMessage(
            exception.getCode(),
            exception.getMessage()
    );
    
    if (environments.contains("dev")) {
        // Includi stack trace in sviluppo
        return HttpResponse.status(...)
                .body(Map.of(
                    "error", message,
                    "stackTrace", exception.getStackTrace()
                ));
    }
    
    return HttpResponse.status(...).body(message);
}
```

### 2. Multilingua

```java
@Error(global = true)
public HttpResponse<ErrorMessage> handleDemoException(
    HttpRequest<?> request, 
    DemoException exception,
    I18nService i18nService
) {
    String locale = request.getHeader("Accept-Language")
        .orElse("en");
    
    String localizedMessage = i18nService.translate(
        exception.getCode(), 
        locale
    );
    
    return HttpResponse.status(HttpStatus.valueOf(exception.getHttpStatus()))
            .body(new ErrorMessage(
                    exception.getCode(),
                    localizedMessage
            ));
}
```

### 3. Metriche di Errore

```java
@Error(global = true)
public HttpResponse<ErrorMessage> handleDemoException(
    HttpRequest<?> request, 
    DemoException exception,
    MeterRegistry meterRegistry
) {
    // Incrementa metrica
    meterRegistry.counter(
        "errors",
        "code", String.valueOf(exception.getCode()),
        "status", String.valueOf(exception.getHttpStatus())
    ).increment();
    
    return HttpResponse.status(HttpStatus.valueOf(exception.getHttpStatus()))
            .body(new ErrorMessage(
                    exception.getCode(),
                    exception.getMessage()
            ));
}
```

## Ordine di Priorità dei Handler

Micronaut seleziona il handler basandosi su:

1. **Match esatto del tipo di eccezione**
2. **Gerarchia di ereditarietà** (dalla più specifica alla più generica)
3. **Ordine di dichiarazione** nel controller

```java
// Ordine consigliato (dal più specifico al più generico):

@Error(global = true)
public HttpResponse<ErrorMessage> handleProductNotFoundException(...) { }

@Error(global = true)
public HttpResponse<ErrorMessage> handleDemoException(...) { }

@Error(global = true)
public HttpResponse<ErrorMessage> handleValidationException(...) { }

@Error(global = true)
public HttpResponse<ErrorMessage> handleGeneralException(...) { }
```

## Testing

```java
@MicronautTest
class ErrorControllerTest {
    
    @Client
    HttpClient client;
    
    @Test
    void testProductNotFound() {
        HttpClientResponseException e = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange("/demo/product/INVALID");
        });
        
        assertEquals(404, e.getStatus().getCode());
        ErrorMessage errorMessage = e.getResponse().getBody(ErrorMessage.class).get();
        assertEquals(404005, errorMessage.getErrorCode());
    }
}
```

## Integrazione con il Framework

Consulta:
- [CRUD Example](../crud/README.md) per operazioni di base con exception handling
- [Distributed Lock Example](../distribuitedlock/README.md) per eccezioni nei lock
- [Communication Example](../communication/README.md) per errori in comunicazioni distribuite
- [ARCHITECTURE.md](../ARCHITECTURE.md) per architettura generale
- [CODING_GUIDELINES.md](../CODING_GUIDELINES.md) per linee guida di codifica
