# CRUD Example - Highways Entity Service

Questo esempio dimostra come implementare operazioni CRUD (Create, Read, Update, Delete) utilizzando il framework `HwEntityService` all'interno di un'applicazione Micronaut.

## Architettura

L'esempio è organizzato nei seguenti layer:

- Le proprietà di HighwaysFactory.java si trova in `application.yml`
- **model**: Definizione delle entità con annotazioni `@HWEntity`
- **controller**: Endpoint HTTP che espongono le operazioni di business
- **service**: Logica di business che usa `HwEntityService`
- **filter**: Oggetti per filtrare e ricercare le entità
- **request/response**: DTO per la comunicazione HTTP

```
┌─────────────┐
│  Controller │ (HTTP Endpoint)
└──────┬──────┘
       │
┌──────▼──────────┐
│   Service       │ (Business Logic)
└──────┬──────────┘
       │
┌──────▼─────────────────────┐
│  HwEntityService            │ (Data Access)
└──────┬─────────────────────┘
       │
┌──────▼──────────┐
│   Persistence   │ (Store)
└─────────────────┘
```

## 1. Definire un'Entità

Crea una classe annotata con `@HWEntity` per rappresentare un'entità nel sistema:

```java
@Data
@HWEntity(HwEntityName.PRODUCT)
@ReflectiveAccess
@Serdeable
public class Product implements IEntity<String> {

    @HWAttribute
    private String id;

    @HWAttribute
    private String sku;

    @HWAttribute
    private String name;

    @HWAttribute
    private Double price;
}
```

**Punti importanti:**
- Usa `@HWEntity` con il nome dell'entità
- Annota ogni campo persistente con `@HWAttribute`
- Implementa `IEntity<K>` dove `K` è il tipo della chiave primaria
- Usa `@Serdeable` per la serializzazione Micronaut
- Usa `@ReflectiveAccess` per l'accesso riflessivo

## 2. Creare un Service

I servizi contengono la logica di business e utilizzano `HwEntityService` per operazioni di persistenza:

```java
@Singleton
public class ProductService {
    
    private final HwEntityService hwEntityService;

    public ProductService(HwEntityService hwEntityService) {
        this.hwEntityService = hwEntityService;
    }

    // ...operazioni CRUD
}
```

## 3. Operazioni CRUD

### CREATE - Creazione di una nuova entità

```java
public ProductResponseTiny createProduct(NewProductRequest product) {
    product.setId(product.getSku());
    return hwEntityService.create(product, ProductResponseTiny.class);
}
```

**Note:**
- `create()` persiste l'entità e restituisce il risultato mappato sulla classe di risposta
- Puoi specificare una classe di risposta diversa per trasformare il risultato

### READ - Lettura per ID

```java
public Product productDetail(String productSku) {
    return hwEntityService.findById(Product.class, productSku)
        .orElseThrow(ProductNotFoundException::new);
}
```

**Note:**
- `findById()` restituisce un `Optional`
- Usa `.orElseThrow()` per gestire il caso non trovato

### UPDATE - Aggiornamento di un'entità

```java
public void updateProduct(String productSku, UpdateProductRequest product) {
    // Verifica che esista
    hwEntityService.findById(Product.class, productSku)
        .orElseThrow(ProductNotFoundException::new);

    product.setId(productSku);
    hwEntityService.upsert(product);
}
```

**Note:**
- `upsert()` aggiorna se esiste, crea se non esiste
- Si consiglia di verificare l'esistenza prima di aggiornare

### LIST - Lettura con filtri e paginazione

```java
public IListResponse<Product> listProducts(ProductFilter filter, PaginationRequest pagination) {
    
    HwCompositeFilter hwFilter = HwFilters.and(
        HwFilters.eq("sku", filter.getSku()),
        HwFilters.gte("price", filter.getFromPrice()),
        HwFilters.lte("price", filter.getToPrice())
    );

    HwQueryOptions options = HwQueryOptions.builder()
        .start(pagination.getStart())
        .limit(pagination.getLimit())
        .includeCount(true)
        .build();

    return hwEntityService.list(Product.class, hwFilter, options);
}
```

#### Con ordinamento e trasformazione risposta

```java
public IListResponse<ProductResponseTiny> listProductsTiny(ProductFilter filter, PaginationRequest pagination) {
    
    HwCompositeFilter hwFilter = HwFilters.and(
        HwFilters.eq("sku", filter.getSku()),
        HwFilters.gte("price", filter.getFromPrice()),
        HwFilters.lte("price", filter.getToPrice())
    );

    HwQueryOptions options = HwQueryOptions.builder()
        .start(pagination.getStart())
        .limit(pagination.getLimit())
        .addSort("sku", HwSortDirection.ASC)
        .includeCount(true)
        .build();

    return hwEntityService.list(Product.class, hwFilter, options, ProductResponseTiny.class);
}
```

**Note:**
- `HwFilters` fornisce operatori: `eq`, `gte`, `lte`, `gt`, `lt`, `contains` etc.
- `HwQueryOptions` consente paginazione, ordinamento e conteggio
- Puoi specificare una classe di risposta per trasformare i risultati
- `includeCount(true)` include il conteggio totale

## 4. Controller - Esporre gli Endpoint

```java
@Controller("demo/product")
@ExecuteOn(TaskExecutors.BLOCKING)
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Post
    public ProductResponseTiny create(@Valid @Body NewProductRequest product) {
        return productService.createProduct(product);
    }

    @Put("{productSku}")
    public void update(String productSku, @Valid @Body UpdateProductRequest product) {
        productService.updateProduct(productSku, product);
    }

    @Get("{productSku}")
    public Product details(String productSku) {
        return productService.productDetail(productSku);
    }

    @Get("{?filter*,pagination*}")
    public IListResponse<Product> list(@Valid ProductFilter filter, PaginationRequest pagination) {
        return productService.listProducts(filter, pagination);
    }
}
```

**Note:**
- Usa `@Controller` per definire il percorso base
- Usa `@ExecuteOn(TaskExecutors.BLOCKING)` per operazioni di I/O
- Valida gli input con `@Valid`
- Usa `@Body` per l'accesso al corpo della richiesta

## 5. Filter - Ricerca e Filtri

Crea una classe filter per i parametri di ricerca:

```java
@ReflectiveAccess
@Serdeable
@Data
public class ProductFilter {
    private String sku;
    
    @PositiveOrZero
    private Double fromPrice;
    
    @PositiveOrZero
    private Double toPrice;
}
```

Nel servizio, usa `HwFilters` per costruire filtri compositi:

```java
HwCompositeFilter filter = HwFilters.and(
    HwFilters.eq("sku", filter.getSku()),
    HwFilters.gte("price", filter.getFromPrice()),
    HwFilters.lte("price", filter.getToPrice())
);
```

## 6. Paginazione

Usa il supporto nativo di `HwQueryOptions`:

```java
HwQueryOptions options = HwQueryOptions.builder()
    .start(pagination.getStart())        // Offset
    .limit(pagination.getLimit())        // Numero di risultati
    .includeCount(true)                   // Includi conteggio totale
    .build();

IListResponse<Product> result = hwEntityService.list(Product.class, filter, options);

// Accedi ai risultati
List<Product> items = result.list();
Long totalCount = result.getCount();
```

## 7. Request e Response

### Request DTO

```java
@Data
@HWEntity(HwEntityName.PRODUCT)
@ReflectiveAccess
@Serdeable
public class NewProductRequest implements IEntity<String> {

    @HWAttribute
    @Null
    private String id;

    @HWAttribute
    @NotEmpty
    @Size(min = 13, max = 13)
    private String sku;

    @HWAttribute
    @Pattern(regexp = "[0-9a-zA-Z '-]{2,40}")
    private String name;

    @HWAttribute
    @Min(0)
    @NotNull
    private Double price;
}
```

### Response DTO

```java
@Data
@Serdeable
public class ProductResponseTiny {
    private String sku;
    private String name;
    private Double price;
}
```

## 8. Gestione delle Eccezioni

Crea eccezioni personalizzate per i casi di errore:

```java
public class ProductNotFoundException extends Exception {
    public ProductNotFoundException() {
        super("Product not found");
    }
}
```

Usa nei servizi:

```java
hwEntityService.findById(Product.class, productSku)
    .orElseThrow(ProductNotFoundException::new);
```

## 9. Transazioni con ITransactionManager

Per operazioni che richiedono atomicità (creare/aggiornare più entità come singola unità), usa `ITransactionManager`:

### Iniettare ITransactionManager

```java
@Singleton
public class ProductService {
    
    private final ITransactionManager transactionManager;

    public ProductService(ITransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    // ...
}
```

### Operazioni Transazionali

```java
// Creare una singola entità in transazione
public Product createProductInTransaction(NewProductRequest request) {
    transactionManager.start();
    try {
        Product product = transactionManager.create(request);
        transactionManager.commit();
        return product;
    } catch (Exception e) {
        transactionManager.rollback();
        throw e;
    }
}

// Creare multiple entità in transazione
public void createMultipleProducts(List<NewProductRequest> requests) {
    transactionManager.start();
    try {
        ArrayNode result = transactionManager.createMultiple(requests);
        transactionManager.commit();
    } catch (Exception e) {
        transactionManager.rollback();
        throw e;
    }
}

// Aggiornamento (upsert) in transazione
public void updateProductInTransaction(UpdateProductRequest request) {
    transactionManager.start();
    try {
        transactionManager.upsert(request);
        transactionManager.commit();
    } catch (Exception e) {
        transactionManager.rollback();
        throw e;
    }
}

// Aggiornamento con trasformazione risposta
public ProductResponseTiny updateAndTransform(UpdateProductRequest request) {
    transactionManager.start();
    try {
        ProductResponseTiny result = transactionManager.upsert(request, ProductResponseTiny.class);
        transactionManager.commit();
        return result;
    } catch (Exception e) {
        transactionManager.rollback();
        throw e;
    }
}

// Aggiornare multiple entità in transazione
public void updateMultipleProducts(List<UpdateProductRequest> requests) {
    transactionManager.start();
    try {
        ArrayNode result = transactionManager.upsertMultiple(requests);
        transactionManager.commit();
    } catch (Exception e) {
        transactionManager.rollback();
        throw e;
    }
}

// Ottenere dettagli di un'entità
public <T> T getDetail(Long entityId, Class<T> responseClass) {
    return transactionManager.detail(entityId, responseClass);
}

// Eliminare un'entità
public void deleteProduct(String entityName, String productId) {
    transactionManager.start();
    try {
        transactionManager.delete(entityName, productId);
        transactionManager.commit();
    } catch (Exception e) {
        transactionManager.rollback();
        throw e;
    }
}
```

### Transazioni con Audit

È possibile registrare eventi di audit durante la transazione:

```java
public void createProductWithAudit(NewProductRequest request) {
    transactionManager.startWithEvent("USER_123", "PRODUCT_CREATE", "User created a product", "API");
    try {
        transactionManager.create(request);
        transactionManager.commit();
    } catch (Exception e) {
        transactionManager.rollback();
        throw e;
    }
}
```

### Operazioni di Listing in Transazione

```java
public IListResponse<Product> listProductsInTransaction(ProductFilter filter, PaginationRequest pagination) {
    transactionManager.start();
    try {
        IListHelper helper = transactionManager.list(Product.class);
        
        // Aggiungere filtri e paginazione tramite helper
        IListResponse<Product> result = helper
            .withFilter(/* filter */)
            .withPagination(pagination.getStart(), pagination.getLimit())
            .execute();
        
        transactionManager.commit();
        return result;
    } catch (Exception e) {
        transactionManager.rollback();
        throw e;
    }
}
```

### Differenza tra HwEntityService e ITransactionManager

| Aspetto | HwEntityService | ITransactionManager |
|--------|-----------------|-------------------|
| **Scopo** | Operazioni di base su singole entità | Raggruppare operazioni atomiche |
| **Transazione** | Auto-commit implicito | Controllo esplicito |
| **Batch** | Possibile | Migliore supporto |
| **Audit** | Limitato | Supporto completo |
| **Rollback** | Non disponibile | Disponibile |

### Best Practices per Transazioni

1. **Usa transazioni per operazioni correlate**: Quando modifichi multiple entità correlate, raggruppa in transazione
2. **Mantieni le transazioni brevi**: Minimizza il tempo di lock su dati
3. **Sempre implementa try-catch-finally**: Assicurati che rollback/commit siano sempre eseguiti
4. **Registra audit events**: Usa `startWithEvent()` per operazioni critiche
5. **Valida prima della transazione**: Esegui validazioni prima di aprire la transazione
6. **Gestisci le eccezioni**: Cattura eccezioni specifiche per log/monitoring

```java
// Pattern consigliato
public Result executeInTransaction(Data data) {
    // Validazione pre-transazione
    validateData(data);
    
    transactionManager.start();
    try {
        // Operazioni
        Result result = performOperations(data);
        transactionManager.commit();
        return result;
    } catch (SpecificException e) {
        transactionManager.rollback();
        log.error("Transaction failed", e);
        throw new BusinessException("Operation failed", e);
    } catch (Exception e) {
        transactionManager.rollback();
        log.error("Unexpected error", e);
        throw e;
    }
}
```

## API Endpoints Disponibili

Con l'implementazione dell'esempio:

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| POST | `/demo/product` | Crea un nuovo prodotto |
| GET | `/demo/product/{productSku}` | Ottiene i dettagli di un prodotto |
| PUT | `/demo/product/{productSku}` | Aggiorna un prodotto |
| GET | `/demo/product?filter.sku=XXX&filter.fromPrice=0&filter.toPrice=100&pagination.start=0&pagination.limit=10` | Lista prodotti con filtri |

## Best Practices

1. **Sempre validare l'input**: Usa le annotazioni di validazione (`@NotNull`, `@NotEmpty`, etc.)
2. **Gestisci gli errori**: Implementa eccezioni personalizzate e mappeale a risposte HTTP
3. **Separa le responsabilità**: Service per la business logic, Controller per gli endpoint
4. **Usa DTO separati**: Request DTO per l'input, Response DTO per l'output
5. **Implementa la paginazione**: Evita di caricare troppi dati
6. **Documenta gli endpoint**: Usa annotazioni come `@ApiOperation` per documentare
7. **Usa trasazioni**: Se necessario, aggiungi `@Transactional`

## Maggiori Informazioni

Consulta:
- [Exceptions ](../exceptions/README.md) per creare e gestire eccezioni
- [CODING_GUIDELINES.md](../CODING_GUIDELINES.md) per le linee guida di codifica
- [ARCHITECTURE.md](../ARCHITECTURE.md) per l'architettura generale
- Vedi l'esempio di comunicazione in [examples/communication](../communication)
