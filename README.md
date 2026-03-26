# Backend Agent - Framework Highways con Micronaut

Benvenuto nel **Backend Agent** di Epipoli! Questa è una guida completa per sviluppare microservizi con il framework **Highways** costruito su **Micronaut**.

## Cos'è il Backend Agent?

Il Backend Agent è una raccolta di esemplificazioni e guide pratiche per sviluppare applicazioni backend robuste, scalabili e sicure utilizzando:

- **Micronaut**: Framework Java moderno e leggero per microservizi
- **Highways Framework**: Framework proprietario di Epipoli per accelerare lo sviluppo
- **Google Cloud**: Integrazione completa con servizi Google Cloud (Cloud Run, GKE, Storage)

## Struttura del Progetto

```
backend-agent/
├── README.md                      # Questo file - Guida introduttiva
├── ARCHITECTURE.md                # Architettura generale del framework
├── CODING_GUIDELINES.md           # Linee guida di codifica
│
├── examples/                      # Esempi pratici e implementazioni
│   ├── communication/             # Comunicazione tra servizi
│   ├── crud/                      # Operazioni CRUD con HwEntityService
│   ├── distribuitedlock/          # Lock distribuiti con Google Cloud Storage
│   ├── exceptions/                # Gestione centralizzata delle eccezioni
│   ├── internalrequest/           # Richieste HTTP a microservizi esterni
│   └── security/                  # Autenticazione e autorizzazione con JWT
│
├── starter-project/               # Progetto starter completo con tutte le dipendenze
│   ├── pom.xml                    # Configuration Maven
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/              # Codice sorgente
│   │   │   └── resources/         # application.yml e configurazioni
│   │   └── test/                  # Test unitari
│   └── Dockerfile                 # Container configuration
│
└── templates/                     # Template reusabili e scaffolding
```

## Quick Start

### 1. Clona o Esplora gli Esempi

Scegli un esempio da cui iniziare:

```bash
cd examples/crud           # Operazioni di database
cd examples/security       # Autenticazione JWT
cd examples/crud           # Transazioni distribuite
cd examples/exceptions     # Error handling
```

### 2. Leggi la Documentazione

Ogni cartella contiene un `README.md` dettagliato:

```bash
cat examples/crud/README.md           # CRUD e HwEntityService
cat examples/security/README.md       # Autenticazione JWT
cat examples/distribuitedlock/README  # Distributed locks
cat examples/exceptions/README.md     # Exception handling
cat examples/internalrequest/README   # Microservice calls
```

### 3. Usa il Progetto Starter

Il `starter-project/` è un progetto completo pronto per iniziare:

```bash
cd starter-project
mvn clean install
mvn mn:run                # Avvia il server
```

## Guida agli Esempi

### 📝 CRUD - Operazioni di Database

**Cartella**: `examples/crud/`

Impara come implementare operazioni CRUD (Create, Read, Update, Delete) usando `HwEntityService`:

- ✅ Creare entità
- ✅ Leggere singoli record
- ✅ Aggiornare entità
- ✅ Elencare con filtri e paginazione
- ✅ Operazioni transazionali con `ITransactionManager`

**Quando usarlo**: Necessiti di operazioni di database standard, persistenza dati, query

**Componenti principali**:
- `@HWEntity` - Annozione per entità
- `HwEntityService` - Servizio di persistenza
- `HwEntityName` - Nomi delle entità
- `HwFilters` - Costruttore di filtri

---

### 🔒 Security - Autenticazione e Autorizzazione

**Cartella**: `examples/security/`

Implementa autenticazione con JWT e controllo d'accesso basato su ruoli:

- ✅ Login con username/password
- ✅ Generazione JWT token
- ✅ Protezione degli endpoint
- ✅ Ruoli e autorizzazione
- ✅ BCrypt password hashing
- ✅ Configurazione YAML

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

- ✅ Lock basati su GCS bucket
- ✅ TTL automatico
- ✅ BlockingLockExecutor per operazioni sincrone
- ✅ LockExecutor per operazioni reactive
- ✅ Gestione della scadenza

**Quando usarlo**: Necessiti di evitare race condition, job distribuiti, operazioni critiche atomiche

**Componenti principali**:
- `BlockingLockExecutor` - Lock sincroni
- `LockExecutor` - Lock reactivi
- `Google Cloud Storage` - Persistenza dei lock
- `TTL` - Scadenza automatica

---

### ⚠️ Exception Handling - Gestione Centralizzata Errori

**Cartella**: `examples/exceptions/`

Implementa un Global Exception Handler per gestire tutte le eccezioni uniformemente:

- ✅ DemoException base
- ✅ Eccezioni specifiche
- ✅ ErrorController globale
- ✅ Response HTTP coerente
- ✅ Logging centralizzato

**Quando usarlo**: Necessiti di gestire errori uniformemente, trasformare eccezioni in risposte HTTP

**Componenti principali**:
- `@Error(global = true)` - Exception handler globale
- `DemoException` - Classe base per le eccezioni
- `ErrorMessage` - DTO per ri isposta di errore
- Eccezioni specifiche (ProductNotFoundException, etc.)

---

### 📡 Internal Request - Chiamate a Microservizi

**Cartella**: `examples/internalrequest/`

Implementa comunicazione sicura tra microservizi usando Google Cloud Identity Tokens:

- ✅ Richieste HTTP ad altri servizi
- ✅ Autenticazione con Google Cloud Credentials
- ✅ Identity Token generation
- ✅ Gestione errori HTTP
- ✅ Retry e timeout

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

- ✅ Comunicazione asincrona
- ✅ Factory pattern
- ✅ Servizi di comunicazione
- ✅ Error handling

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
    HwFilters.eq("sku", "ABC123"),
    HwFilters.gte("price", 10.0),
    HwFilters.lte("price", 100.0)
)
```

### Annotazioni Principali

```java
@HWEntity(HwEntityName.PRODUCT)    // Definisce un'entità
@HWAttribute                        // Campo persistente
@Secured({RoleType.ROLE_API})      // Protegge endpoint
@Error(global = true)              // Exception handler globale
@Singleton                          // Bean Micronaut singleton
```

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

# Docker build
docker build -t my-service:latest .
docker run -p 8080:8080 my-service:latest
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

1. ❌ Password in chiaro - sempre usare BCrypt
2. ❌ Endpoint senza autenticazione - proteggi sempre
3. ❌ Debug exception catching - gestisci gli errori appropriatamente
4. ❌ Query n+1 - usa JOIN e eager loading dove opportuno
5. ❌ Magic string - usa costanti e enum
6. ❌ Bloccare thread worker - usa Micronaut async patterns
7. ❌ Esporre dettagli interni - risposte di errore generiche

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

## Deployment

### Google Cloud Run

```bash
# Build image
gcloud builds submit --tag gcr.io/PROJECT_ID/my-service .

# Deploy
gcloud run deploy my-service \
  --image gcr.io/PROJECT_ID/my-service \
  --platform managed \
  --region europe-west1
```

### Kubernetes (GKE)

```bash
# Push immagine
docker tag my-service:latest gcr.io/PROJECT_ID/my-service:latest
docker push gcr.io/PROJECT_ID/my-service:latest

# Deploy
kubectl apply -f deployment.yaml
```

## Risorse Utili

### Documentazione

- [Micronaut Framework](https://micronaut.io/)
- [JWT Introduction](https://jwt.io/)
- [Google Cloud Documentation](https://cloud.google.com/docs)

### Guide Correlate

- [ARCHITECTURE.md](ARCHITECTURE.md) - Architettura generale del framework
- [CODING_GUIDELINES.md](CODING_GUIDELINES.md) - Linee guida di codifica

### Tools e Utilità

```bash
# Test API locali
curl -X GET http://localhost:8080/api/endpoint

# View logs in container
docker logs -f container-id

# Monitor GCP deployment
gcloud run services describe my-service

# Debug database
cloud sql proxy -instances=PROJECT:REGION:INSTANCE &
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

**D: Come testo il mio servizio?**
R: Scrivi test in `src/test/java/` - Vedi `examples/` per pattern

**D: Come configuro le variabili d'ambiente?**
R: Usa `${VAR_NAME:default-value}` in application.yml

**D: Come deplopo in production?**
R: Vedi sezione Deployment - Google Cloud Run o GKE

## Supporto e Contributi

Per domande o miglioramenti, contatta il team di sviluppo Epipoli.

---

**Buona codifica!** 🚀

