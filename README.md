# Backend Agent - Framework Highways con Micronaut

Benvenuto nel **Backend Agent** di Epipoli! Questa è una guida completa per sviluppare microservizi con il framework **Highways** costruito su **Micronaut**.


Progetto Base




## Quick Start

### 3. Usa il Progetto Starter

Il `starter-project/` è un progetto blank completo pronto per iniziare, copialo e sostituisci nome app e artifactId

```bash
cd starter-project
mvn clean install
mvn mn:run                # Avvia il server
```


### 🔹 Feature-Specific Configuration

Aggiungi SOLO le proprietà richieste dal tuo use case:

#### Se usi **CRUD** (examples/crud/) per salvare i dati in database
```yaml
highways:
  datastore:
    datastoreId: ${DATASTORE_ID:datastore-eu}
    namespace: ${DATASTORE_NAMESPACE:demo}
    projectId: ${DATASTORE_PROJECT_ID:pocketwallet-dev-srv-epipoli}
```
**Checklist:**
- [ ] Datastore ID configurato
- [ ] Namespace specificato
- [ ] Project ID allineato con gcloud.projectId

#### Se usi **COMUNICAZIONI** (examples/communication/) per inviare comunicazioni
```yaml
highways:
  communication:
    template: 
      projectId: 
      datastoreId: 
      namespace: 
    data: 
      projectId: 
      datastoreId: 
      namespace: 
    topic:
      id: 
      projectId: 

```

#### Se usi **Distributed Lock** (examples/distribuitedlock/)
```yaml
locks:
  bucket: ${LOCKS_BUCKET:locks-pocketwallet-dev-srv-epipoli}
  ttl: ${LOCKS_TTL:300}
```
**Checklist:**
- [ ] GCS bucket per i lock creato
- [ ] TTL impostato (secondi)
- [ ] Bucket permissions configurate

#### Se usi **Security JWT** (examples/security/)
```yaml
micronaut:
  security:
    authentication: bearer
    token:
      jwt:
        signatures:
          secret:
            generator:
              secret: ${JWT_GENERATOR_SIGNATURE_SECRET:changeThisSecret}
    interceptUrlMap:
      - pattern: /health/**
        access:
          - isAnonymous()
      - pattern: /swagger/**
        httpMethod: GET
        access:
          - isAnonymous()
      - pattern: /**
        access:
          - isAuthenticated()
```
**Checklist:**
- [ ] JWT_GENERATOR_SIGNATURE_SECRET impostato (PRODUZIONE: env var)
- [ ] Pattern degli endpoint configurati
- [ ] Public endpoints (health, swagger) consentiti
- [ ] Tutti gli altri endpoint richiedono autenticazione

#### Se comunichi con **microservizi interni** (examples/internalrequest/)
```yaml
# Eredita da base configuration
# Assicurati che GOOGLE_APPLICATION_CREDENTIALS sia configurato
```
**Checklist:**
- [ ] Google Cloud credentials file presente
- [ ] GOOGLE_APPLICATION_CREDENTIALS env var configurata
- [ ] Service account con permessi corretti

### 📋 Ambiente-Specific Overrides

```

Attiva con: `java -jar app.jar -Dmicronaut.environments=prod`


## Guida agli Esempi

### 📝 CRUD - Operazioni di Database

**Cartella**: `examples/crud/`

Impara come implementare operazioni CRUD (Create, Read, Update, Delete) usando `HwEntityService`:

-  Creare entità
-  Leggere singoli record
-  Aggiornare entità
-  Elencare con filtri e paginazione
-  Operazioni transazionali con `ITransactionManager`

**Quando usarlo**: Necessiti di operazioni di database standard, persistenza dati, query

**Componenti principali**:
- `@HWEntity` - Annozione per entità
- `HwEntityService` - Servizio di persistenza
- `HwEntityName` - Nomi delle entità 
- `HwFilters` - Costruttore di filtri

**Esempio HwEntityName**
```java
public interface HwEntityName {
    public String CUSTOMER = "customer";
    public String PRODUCT = "product";
    public String ORDER = "order";
}
```
---

### 🔒 Security - Autenticazione e Autorizzazione

**Cartella**: `examples/security/`

Implementa autenticazione con JWT e controllo d'accesso basato su ruoli:

-  Login con username/password
-  Generazione JWT token
-  Protezione degli endpoint
-  Ruoli e autorizzazione
-  BCrypt password hashing
-  Configurazione YAML

**Quando usarlo**: Necessiti di autenticazione API, protezione degli endpoint, gestione dei ruoli

**Componenti principali**:
- `@Secured` - Annotazione per proteggere endpoint
- `JwtTokenGenerator` - Generazione token JWT
- `BCrypt` - Hashing delle password
- `Authentication` - Context di autenticazione

---

### 🔀 Distributed Lock - Coordinamento Distribuito

**Cartella**: `examples/distribuitedlock/`

Implementa lock distribuiti per coordinare operazioni critiche tra più istanze:

-  Lock basati su GCS bucket
-  TTL automatico
-  BlockingLockExecutor per operazioni sincrone
-  LockExecutor per operazioni reactive
-  Gestione della scadenza

**Quando usarlo**: Necessiti di evitare race condition, job distribuiti, operazioni critiche atomiche

**Componenti principali**:
- `BlockingLockExecutor` - Lock sincroni
- `LockExecutor` - Lock reactivi
- `Google Cloud Storage` - Persistenza dei lock
- `TTL` - Scadenza automatica

---

### ⚠ Exception Handling - Gestione Centralizzata Errori

**Cartella**: `examples/exceptions/`

Implementa un Global Exception Handler per gestire tutte le eccezioni uniformemente:

-  DemoException base
-  Eccezioni specifiche
-  ErrorController globale
-  Response HTTP coerente
-  Logging centralizzato

**Quando usarlo**: Necessiti di gestire errori uniformemente, trasformare eccezioni in risposte HTTP

**Componenti principali**:
- `@Error(global = true)` - Exception handler globale
- `DemoException` - Classe base per le eccezioni
- `ErrorMessage` - DTO per ri isposta di errore
- Eccezioni specifiche (ProductNotFoundException, etc.)

---

### Internal Request - Chiamate a Microservizi

**Cartella**: `examples/internalrequest/`

Implementa comunicazione sicura tra microservizi usando Google Cloud Identity Tokens:

-  Richieste HTTP ad altri servizi
-  Autenticazione con Google Cloud Credentials
-  Identity Token generation
-  Gestione errori HTTP
-  Retry e timeout

**Quando usarlo**: Necessiti di chiamare altri microservizi in modo sicuro, comunicazione service-to-service

**Componenti principali**:
- `GoogleCredentials` - Credenziali Google Cloud
- `HttpClient` - Client HTTP Micronaut
- `IdTokenCredentials` - Token di identità
- `HttpRequest` - Builder per richieste HTTP

---

### 💬 Communication - Comunicazione tra Servizi

**Cartella**: `examples/communication/`

Implementa pattern di comunicazione tra servizi usando factory e servizi dedicati:

-  Comunicazione asincrona
-  Factory pattern
-  Servizi di comunicazione
-  Error handling

**Quando usarlo**: Necessiti di comunicare tra componenti, publish-subscribe pattern

---

## Concetti Chiave del Framework

### HwEntityService

Il cuore della persistenza. Fornisce metodi per:

```java
hwEntityService.create(entity)              // Crea
hwEntityService.findById(Class, id)         // Legge
hwEntityService.upsert(entity)              // Aggiorna o crea
hwEntityService.list(Class, filter, opts)   // Elenca
```

### ITransactionManager

Per operazioni atomiche raggruppate:

```java
transactionManager.start()
    // Operazioni multiple
transactionManager.commit()    // o rollback()
```

### HwFilters

Costruttore di query fluido:

```java
     HwFilters.and(
        HwFilters.eq("code", filter.getCode()),
        HwFilters.eq("state", filter.getState()),
        HwFilters.eq("orderId", filter.getOrderId()),
        HwFilters.eq("companyId", filter.getCompanyId()),
        HwFilters.eq("typologyId", filter.getTypologyId()),

        //Caso Instant
        HwFilters.gte("activationDate", Optional.ofNullable(filter.getActivationDate())
            .map(i -> i.truncatedTo(ChronoUnit.DAYS)).orElse(null)),
        HwFilters.lt("activationDate", Optional.ofNullable(filter.getActivationDate())
            .map(i -> i.truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS)).orElse(null)),

        HwFilters.gte("expireDate", Optional.ofNullable(filter.getExpireDate())
            .map(i -> i.truncatedTo(ChronoUnit.DAYS)).orElse(null)),
        HwFilters.lt("expireDate", Optional.ofNullable(filter.getExpireDate())
            .map(i -> i.truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS)).orElse(null)),

        HwFilters.gte("sequentialNumber", filter.getSequentialNumberFrom()),
        HwFilters.lte("sequentialNumber", filter.getSequentialNumberTo()),

        HwFilters.or(
            HwFilters.icontains("activationStoreCode", filter.getActivationStore()),
            HwFilters.icontains("activationStoreName", filter.getActivationStore())
        ),
        HwFilters.icontains("orderNumber", filter.getOrderNumber())
    );
```

### Annotazioni Principali

```java
@HWEntity(HwEntityName.PRODUCT)    // Definisce un'entità
@HWAttribute                        // Campo persistente
@Secured({RoleType.ROLE_API})      // Protegge endpoint
@Error(global = true)              // Exception handler globale
@Singleton                          // Bean Micronaut singleton
@Factory                            // Factory di bean Highways
@Bean                               // Bean Micronaut fornito dalla factory
```

### 🏭 HighwaysFactory - Provisioning dei Bean (OBBLIGATORIO)

**Cos'è**: Una `@Factory` class che configura e fornisce i bean critici di Highways Framework (CrudService, HwEntityService, Datastore, GoogleCredentials) al contenitore di dependency injection di Micronaut.

**Perché è obbligatorio**: Senza HighwaysFactory, non è possibile:
- Iniettare `HwEntityService` nei servizi
- Accedere a Google Cloud Credentials
- Configurare Google Cloud Datastore
- Usare CrudService

**Posizione**: `src/main/java/com/epipoli/{projectname}/factory/HighwaysFactory.java`

**Struttura base**:

```java
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Bean;
import org.highways.sdk.base.CrudService;
import org.highways.sdk.base.HwEntityService;
// ... altri import

@Factory
public class HighwaysFactory {
    
    private final GoogleCredentials googleCredentials;
    private final Datastore datastore;
    private final String namespace;
    
    public HighwaysFactory(
        @Value("${gcloud.project-id}") String projectId,
        @Value("${highways.datastore.datastore-id}") String datastoreId,
        @Value("${highways.datastore.namespace}") String namespace
    ) throws IOException {
        this.googleCredentials = loadGoogleCredentials();
        this.datastore = Datastore.newBuilder()
            .setProjectId(projectId)
            .setDatabaseId(datastoreId)
            .build();
        this.namespace = namespace;
    }
    
    @Bean
    public CrudService crudService() {
        return new CrudService(new DatastoreConfig(datastore, namespace));
    }
    
    @Bean
    public HwEntityService hwEntityService() {
        return new HwEntityService(crudService());
    }
    
    private GoogleCredentials loadGoogleCredentials() throws IOException {
        String credentialsPath = System.getenv("GCLOUD_CREDENTIALS");
        if (credentialsPath != null && !credentialsPath.isEmpty()) {
            return GoogleCredentials.fromStream(
                new FileInputStream(credentialsPath));
        }
        return GoogleCredentials.getApplicationDefault();
    }
}
```

**Configura in application.yml**:

```yaml
gcloud:
  project-id: ${GCLOUD_PROJECT_ID}

highways:
  datastore:
    datastore-id: my-datastore
    namespace: default-namespace
```

**Gestisci credenziali**:

```bash
# Opzione 1: File di credenziali
export GCLOUD_CREDENTIALS=/path/to/service-account.json

# Opzione 2: Application Default Credentials
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json
```

---

### 🎯 Servizi con HwEntityService - Pattern (OBBLIGATORIO)

**Cos'è**: I servizi devono iniettare `HwEntityService` per eseguire operazioni CRUD su Datastore. Questo è il pattern standard per tutta la logica di persistenza.

**Perché**: 
- Separazione tra logica di business (service) e persistenza
- Riutilizzo di HwEntityService fornito da HighwaysFactory
- Consistenza con il pattern Highways Framework

**Posizione**: `src/main/java/com/epipoli/{projectname}/services/{EntityName}Service.java`

**Struttura base**:

```java
import com.epipoli.commons.repository.HwEntityService;
import com.epipoli.commons.queryfilter.HwFilters;
import com.epipoli.commons.queryfilter.HwQueryOptions;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ProductService {
    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    
    private final HwEntityService hwEntityService;
    
    // Iniettare HwEntityService nel costruttore
    public ProductService(HwEntityService hwEntityService) {
        this.hwEntityService = hwEntityService;
    }
    
    // CREATE: Creare una nuova entità
    public Product createProduct(Product product) {
        product.setId(null); // ID autogenerato da Datastore
        Product created = hwEntityService.create(product);
        logger.info("Prodotto creato: id={}, sku={}", created.getId(), created.getSku());
        return created;
    }
    
    // READ: Leggere un singolo record per ID
    public Product getProduct(String productId) {
        return hwEntityService.findById(Product.class, productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));
    }
    
    // READ: Cercare per campo specifico (es. SKU)
    public Product findBySku(String sku) {
        var result = hwEntityService.list(
            Product.class,
            HwFilters.eq("sku", sku),
            HwQueryOptions.builder().limit(1).build()
        );
        if (result.getList().isEmpty()) {
            throw new ProductNotFoundException("SKU: " + sku);
        }
        return result.getList().get(0);
    }
    
    // UPDATE: Aggiornare un'entità
    public void updateProduct(String productId, Product product) {
        product.setId(productId); // Imposta l'ID
        hwEntityService.findById(Product.class, productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));
        hwEntityService.upsert(product);
        logger.info("Prodotto aggiornato: id={}", productId);
    }
    
    // LIST: Elencare con filtri e paginazione
    public IListResponse<Product> listProducts(String skuFilter, int start, int limit) {
        var filter = HwFilters.and(
            HwFilters.eq("sku", skuFilter),
            HwFilters.gte("price", 0.0)
        );
        
        var options = HwQueryOptions.builder()
            .start(start)
            .limit(limit)
            .includeCount(true)
            .build();
        
        return hwEntityService.list(Product.class, filter, options);
    }
}

//dove IListResponse già presente in hw-crud 
```
public interface IListResponse<T> {
   Long getSize();

   List<T> getResults();

   void setSize(Long var1);

   void setResults(List<T> var1);
}
```

**Pattern di entità**:

L'entità deve avere le annotazioni Highways:

```java
import com.epipoli.commons.annotation.HWAttribute;
import com.epipoli.commons.annotation.HWEntity;
import com.epipoli.commons.interfaces.IEntity;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Serdeable
@ReflectiveAccess //Sempre presente nei payload in ingresso e uscita
@Introspected //Sempre presente nei payload in ingresso e uscita
@HWEntity(HwEntityName.CUSTOMER)  // o PRODUCT, ORDER, etc.
public class Customer implements IEntity<Long> {
     @HWAttribute
    private String id;

    @HWAttribute
    private String name;

    @HWAttribute
    private Boolean boolValue;

    @HWAttribute
    private Double doubleValue;

    @HWAttribute
    private Long longValue;

    @HWAttribute //Crea struttura array su datastore
    private List<String> listString;

    @HWAttribute //Crea struttura array su datastore
    private List<Long> listLong;

    @HWAttribute //Crea hashmap come embedded entity
    HashMap<String, String> map;

    @HWAttribute //Serializza settings come embedded entity
    Settings settings;

    @HWAttribute //Crea un array di embedded entity
    List<Settings> settingsList;

    @HWAttribute
    private Instant instant;

    @HWAttribute(type = "bcrypt") //Se viene passata una stringa viene automaticamente criptata, se viene passata una stringa gia criptata non viene trasformata
    private String autoBcrypt;

    @HWAttribute //Crea embedded entity
    private ObjectNode jsonValue;

    @HWAttribute (type = "json") //crea un text json
    private ObjectNode jsonValue;

    @HWAttribute  //Crea array embedded entity
    private ArrayNode arrayJsonValue;

    @HWAttribute(links = {"company"})
    private Long companyId;

    @HWAttribute(links = {"company"}) //Solo in lettura
    private Company company;
}
```

**Checklist per i Servizi**:

- [ ] Servizio ha `@Singleton`
- [ ] HwEntityService iniettato nel costruttore
- [ ] Usa `hwEntityService.create()` per creare
- [ ] Usa `hwEntityService.findById()` per leggere singolo record
- [ ] Usa `hwEntityService.list()` con HwFilters per cercare
- [ ] Usa `hwEntityService.upsert()` per aggiornare
- [ ] Eccezioni specifiche lanciate quando record non trovato
- [ ] Logging appropriato per operazioni critiche
- [ ] Entità ha `@HWEntity`, `@HWAttribute`, `@Serdeable`
- [ ] Entità implementa `IEntity<TipoChiave>`

**Esempi Consultare**:

Vedi `examples/crud/` per implementazioni complete:
- `CustomerService.java` - Pattern basic CRUD
- `ProductService.java` - Filtri e paginazione
- `OrderService.java` - Transazioni con `ITransactionManager`

---

## Come Navigare Questo Repository

### Per fare una cosa... leggi qui:

| Compito | Cartella | File |
|--------|----------|------|
| Creare un'API CRUD | `examples/crud/` | `ProductController.java`, `ProductService.java` |
| Proteggere un endpoint | `examples/security/` | `AuthController.java`, `RoleType.java` |
| Gestire errori | `examples/exceptions/` | `ErrorController.java`, `DemoException.java` |
| Coordinare istanze | `examples/distribuitedlock/` | `BlockingLockExecutor.java` |
| Chiamare altro microservizio | `examples/internalrequest/` | `InternalRequestController.java` |
| Iniziare un nuovo progetto | `starter-project/` | `pom.xml`, `Application.java` |

## Configurazione Iniziale

### 1. Configura application.yml

Copia il template e configura:

```yaml
micronaut:
  application:
    name: my-service
  
  security:
    authentication: bearer
    token:
      jwt:
        signatures:
          secret:
            generator:
              secret: ${JWT_GENERATOR_SIGNATURE_SECRET:change-me}
  
  gcp:
    project-id: ${GCP_PROJECT_ID}
```

### 2. Imposta Variabili d'Ambiente

```bash
export GCP_PROJECT_ID=my-project
export JWT_GENERATOR_SIGNATURE_SECRET=$(openssl rand -base64 32)
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json
```

### 3. Compilazione e Lancio

```bash
# Build
mvn clean package

# Esecuzione locale
mvn mn:run

```

## Best Practices

### ✅ Fai

1. **Valida sempre l'input**: Usa `@Valid`, `@NotNull`, `@NotEmpty`
2. **Gestisci gli errori**: Crea eccezioni specifiche, usa Global Handler
3. **Proteggi gli endpoint**: Usa `@Secured` con ruoli appropriati
4. **Docifica il codice**: Commenti su logica complessa, Javadoc su API pubbliche
5. **Testa il codice**: Unit test per service, integration test per controller
6. **Monitora**: Aggiungi logging appropriato
7. **Usa le transazioni**: Per operazioni correlate multiple

### ❌ Non fare

1. Password in chiaro - sempre usare BCrypt
2.  Endpoint senza autenticazione - proteggi sempre
3.  Debug exception catching - gestisci gli errori appropriatamente
4.  Query n+1 - usa JOIN e eager loading dove opportuno
5.  Magic string - usa costanti e enum
6.  Bloccare thread worker - usa Micronaut async patterns
7.  Esporre dettagli interni - risposte di errore generiche

## Struttura di un Endpoint Tipico

```
HTTP Request (POST /api/products)
        ↓
Controller (@Controller)
  ├─ Validazione (@Valid)
  ├─ Parsing parametri
  └─ Chiama Service
        ↓
Service (@Singleton)
  ├─ Business logic
  ├─ Usa HwEntityService
  └─ Ritorna risultato
        ↓
Response (@ToString per debug)
  ├─ Codice HTTP (200, 404, 400, 500)
  ├─ Body (DTO)
  └─ Header

Se errore:
  ├─ Lancia eccezione personalizzata
        ↓
  ├─ ErrorController intercetta
  └─ Restituisce ErrorMessage JSON
```

## Risorse Utili

### Documentazione

- [Micronaut Framework](https://micronaut.io/)
- [JWT Introduction](https://jwt.io/)
- [Google Cloud Documentation](https://cloud.google.com/docs)

### Tools e Utilità

```bash
# Test API locali
curl -X GET http://localhost:8080/api/endpoint
```

## FAQ per l'Agente

**D: Come aggiungo un nuovo endpoint CRUD?**
R: Vedi `examples/crud/` - Crea Controller + Service + Model + Filter

**D: Come proteggo un endpoint?**
R: Usa `@Secured({RoleType.ROLE_API})` - Vedi `examples/security/`

**D: Come gestisco gli errori?**
R: Crea eccezione in `examples/exceptions/` - ErrorController li cattura automaticamente

**D: Come chiamo un altro microservizio?**
R: Usa `InternalRequestController` pattern in `examples/internalrequest/`

**D: Come faccio lock distribuiti?**
R: Vedi `examples/distribuitedlock/` - BlockingLockExecutor o LockExecutor

**D: Come configuro le variabili d'ambiente?**
R: Usa `${VAR_NAME:default-value}` in application.yml

---

**Buona codifica!** 🚀

