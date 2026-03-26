# Distributed Lock Example - Google Cloud Storage Bucket

Questo esempio dimostra come implementare lock distribuiti utilizzando Google Cloud Storage (GCS) bucket per coordinare operazioni critiche in un'architettura distribuita.

## Concetti Fondamentali

Un **distributed lock** impedisce a più istanze dell'applicazione di eseguire la stessa operazione simultaneamente. Questo è essenziale quando:

- Più pod/server eseguono la stessa logica
- Vuoi evitare race condition
- Devi garantire che un'operazione critica sia atomica
- Hai operazioni che non devono essere eseguite in parallelo (es. importazioni, sincronizzazioni)

## Architettura

```
┌─────────────────────────────────────────┐
│   Applicazione Distribuita              │
│  (Multiple istanze/pod)                 │
└─────────────────────────────────────────┘
          │           │           │
          ▼           ▼           ▼
    ┌──────────────────────────────┐
    │  BlockingLockExecutor        │
    │  LockExecutor (Reactive)     │
    └──────────────┬───────────────┘
                   │
              ┌────▼─────────────────┐
              │  Google Cloud Storage │
              │  (Lock Bucket)        │
              └──────────────────────┘
```

## Configurazione

### application.yml

```yaml
locks:
  bucket: ${LOCKS_BUCKET:locks-pocketwallet-dev-srv-epipoli}
  ttl: ${LOCKS_TTL:300}
```

**Parametri:**
- `locks.bucket`: Nome del bucket GCS dove memorizzare i lock
- `locks.ttl`: Time To Live del lock in secondi (scadenza automatica)

### Variabili di Ambiente

```bash
export LOCKS_BUCKET=my-locks-bucket
export LOCKS_TTL=300  # 5 minuti
```

## Componenti Principali

### 1. BlockingLockExecutor (Operazioni Sincrone)

Per operazioni sincrone/blocking:

```java
@Singleton
public class BlockingLockExecutor {
    
    public <T> T withLock(String id, Supplier<T> action) {
        // Acquisisci lock
        // Esegui azione
        // Rilascia lock
    }
}
```

**Utilizzo nel Controller:**

```java
@Controller("demo/lock")
@ExecuteOn(TaskExecutors.BLOCKING)
public class DemoLockBlockingController {

    private final BlockingLockExecutor blockingLockExecutor;

    @Get("blocking/{sku}")
    public Product lock(String sku) {
        String lockId = String.format("%s_lock", sku);

        Product result = blockingLockExecutor.withLock(lockId, () -> {
            // Operazione protetta dal lock
            return entityService.findById(Product.class, sku)
                .orElseThrow(ProductNotFoundException::new);
        });

        return result;
    }
}
```

### 2. LockExecutor (Operazioni Reactive)

Per operazioni async/reactive con Project Reactor:

```java
@Singleton
public class LockExecutor {
    
    public <T> Mono<T> withLock(String id, Supplier<Mono<T>> action) {
        // Acquisisci lock in modo reactive
        // Esegui azione 
        // Rilascia lock
    }
}
```

**Utilizzo nel Controller:**

```java
@Controller("demo/lock")
@ExecuteOn(TaskExecutors.IO)
public class DemoLockReactiveController {

    private final LockExecutor lockExecutor;

    @Get("reactive/{sku}")
    public Mono<Product> reactiveLock(String sku) {
        String lockId = String.format("%s_lock", sku);

        return lockExecutor.withLock(lockId, () ->
            Mono.delay(Duration.ofSeconds(30))
                .flatMap(tick -> Mono.fromCallable(() -> 
                    entityService.findById(Product.class, sku)
                        .orElseThrow(ProductNotFoundException::new)
                ))
        );
    }
}
```

## Meccanismo di Lock

### Acquisizione del Lock

1. **Verifica esistenza**: Cerca il blob del lock nel bucket
2. **Controlla scadenza**: Se il blob esiste, legge il metadata `expiresAt`
3. **Lock scaduto?**: Se `now > expiresAt`, lo elimina (lock scaduto, riacquisionabile)
4. **Lock attivo**: Se il lock esiste e non è scaduto, lancia `RequestAlreadyInProgressException`
5. **Crea lock**: Se non esiste, crea un nuovo blob con metadata `expiresAt`

### Rilascio del Lock

1. **Cancella il blob**: Elimina il blob del lock dal bucket
2. **Finalizzazione**: Garantisce il rilascio anche in caso di eccezione (try-finally)

### Ciclo di Vita

```
┌─────────────────────────────────────┐
│  Richiesta Client                   │
└────────────┬────────────────────────┘
             │
             ▼
┌─────────────────────────────────────┐
│  Prova ad acquisire il lock         │
└────────────┬────────────────────────┘
             │
        ┌────┴───────────┐
        │                │
        ▼                ▼
    LOCK OK         LOCK FALLITO
        │           (già acquisito)
        │                │
        ▼                ▼
  ┌──────────┐    ┌─────────────────┐
  │ Esegui   │    │ Throw Exception │
  │ Action   │    │ (409 Conflict?) │
  └────┬─────┘    └─────────────────┘
       │
       ▼
  ┌──────────┐
  │ Rilascia │
  │ Lock     │
  └────┬─────┘
       │
       ▼
┌─────────────────────────────────────┐
│  Risposta al Client                 │
└─────────────────────────────────────┘
```

## Gestione delle Eccezioni

### RequestAlreadyInProgressException

Viene lanciata quando il lock è già acquisito da un'altra istanza:

```java
try {
    Product product = blockingLockExecutor.withLock(lockId, () -> {
        return someService.process();
    });
} catch (RequestAlreadyInProgressException e) {
    // Il lock era già acquisito
    // Opzioni: retry, fallback, errore al client
    return Response.status(409, "Operation already in progress");
}
```

## Casi di Utilizzo

### 1. Sincronizzazione di Importazioni

```java
public void importDataFromExternalAPI() {
    String lockId = "api_import_lock";
    
    blockingLockExecutor.withLock(lockId, () -> {
        List<Item> items = externalAPI.getItems();
        items.forEach(item -> database.insert(item));
        return null;
    });
}
```

### 2. Job Schedulati Distribuiti

```java
@Scheduled(fixed = "5m")
public void scheduledJob() {
    String lockId = "daily_batch_job";
    
    try {
        blockingLockExecutor.withLock(lockId, () -> {
            // Solo UNA istanza eseguirà questo job
            return performBatchProcessing();
        });
    } catch (RequestAlreadyInProgressException e) {
        log.info("Job già in esecuzione su un'altra istanza");
    }
}
```

### 3. Operazioni Critiche in Cascade

```java
public void processOrder(Order order) {
    String lockId = "order_" + order.getId();
    
    blockingLockExecutor.withLock(lockId, () -> {
        // Operazioni atomiche sull'ordine
        validateOrder(order);
        reserveInventory(order);
        createShipment(order);
        sendNotification(order);
        return null;
    });
}
```

### 4. Operazioni Reactive

```java
public Mono<ProcessResult> processAsyncWithLock(String resourceId) {
    String lockId = "async_process_" + resourceId;
    
    return lockExecutor.withLock(lockId, () ->
        externalService.processAsync(resourceId)
            .flatMap(this::handleResult)
            .onErrorResume(this::handleError)
    );
}
```

## Best Practices

### 1. Scegli ID di Lock Significativi

```java
// ✅ Buono: Identificativo univoco della risorsa
String lockId = "inventory_" + productId;

// ❌ Cattivo: Lock troppo generico
String lockId = "global_lock";

// ❌ Cattivo: Lock troppo specifico (ogni lock è diverso)
String lockId = UUID.randomUUID().toString();
```

### 2. Mantieni le Operazioni Brevi

```java
// ❌ Cattivo: Lock tenuto troppo a lungo
blockingLockExecutor.withLock(id, () -> {
    data = database.query();
    data.forEach(item -> {
        externalAPI.call(item);  // I/O lungo!
        Thread.sleep(1000);      // Delay!
    });
    return data;
});

// ✅ Buono: Solo operazioni critiche dentro il lock
blockingLockExecutor.withLock(id, () -> {
    return database.updateStatus(PROCESSING);
});
// Poi fare operazioni lunghe fuori dal lock
```

### 3. Gestisci le Eccezioni Appropriatamente

```java
try {
    return blockingLockExecutor.withLock(lockId, action);
} catch (RequestAlreadyInProgressException e) {
    // Retry con backoff esponenziale
    return retryWithBackoff(lockId, action);
} catch (Exception e) {
    log.error("Errore durante lock execution", e);
    throw new CriticalOperationException(e);
}
```

### 4. TTL Appropriato

```yaml
locks:
  ttl: 300  # 5 minuti per operazioni normali
           # Se scade durante l'esecuzione = problema!
```

**Regola di dimensionamento:**
- TTL > max tempo di esecuzione dell'operazione + margine di sicurezza
- Altrimenti il lock scade prima che l'operazione finisca

### 5. Logging Dettagliato

```java
public <T> T withLock(String id, Supplier<T> action) {
    log.info("Tentativo di acquisire lock: {}", id);
    
    try {
        acquire(id);
        log.info("Lock acquisito: {}", id);
        return action.get();
    } catch (RequestAlreadyInProgressException e) {
        log.warn("Lock non disponibile: {}", id);
        throw e;
    } finally {
        release(id);
        log.info("Lock rilasciato: {}", id);
    }
}
```

## Blocking vs Reactive

| Aspetto | BlockingLockExecutor | LockExecutor (Reactive) |
|--------|----------------------|------------------------|
| **Tipo di operazioni** | Sincrone/Blocking | Async/Reactive |
| **Executor** | `TaskExecutors.BLOCKING` | `TaskExecutors.IO` |
| **Ritorno** | `T` o throws | `Mono<T>` |
| **Azione** | `Supplier<T>` | `Supplier<Mono<T>>` |
| **Ideale per** | Database sync, API REST | Operazioni async, streaming |
| **Scalabilità** | Thread pool limitato | Numero thread ridotto |

## Monitoring e Observability

### Metriche Suggerite

- Tempo di acquisizione del lock
- Numero di lock falliti (RequestAlreadyInProgressException)
- Tempo di esecuzione dell'azione protetta
- TTL vs tempo effettivo di utilizzo

### Esempio di Metriche

```java
@Singleton
public class MonitoredLockExecutor {
    
    private final MeterRegistry meterRegistry;
    private final BlockingLockExecutor lockExecutor;
    
    public <T> T withMonitoredLock(String id, Supplier<T> action) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            return lockExecutor.withLock(id, action);
        } catch (RequestAlreadyInProgressException e) {
            meterRegistry.counter("lock.failed", "id", id).increment();
            throw e;
        } finally {
            sample.stop(Timer.builder("lock.duration")
                .tag("id", id)
                .register(meterRegistry));
        }
    }
}
```

## Troubleshooting

### Lock non viene acquisito

1. Verifica che il bucket GCS esista
2. Verifica credenziali Google Cloud
3. Verifica permessi su bucket (`storage.buckets.get`, `storage.objects.*`)
4. Verifica configurazione TTL (non troppo breve)

### Lock rimane acquisito dopo l'operazione

1. Verifica che il `finally` nel executor sia eseguito
2. Controlla i log per eccezioni
3. Pulisci manualmente il bucket (blob scaduti)

### Scadenza del lock durante l'operazione

1. **Aumenta TTL** se le operazioni sono lunghe
2. **Riduci il tempo** dell'operazione critica
3. Considera un sistema di **lock renewal** per operazioni molto lunghe

## Integrazione con il Framework

Consulta:
- [Exceptions ](../exceptions/README.md) per creare e gestire eccezioni
- [CRUD Example](../crud/README.md) per operazioni di base
- [Communication Example](../communication/README.md) per servizi distribuiti
- [ARCHITECTURE.md](../ARCHITECTURE.md) per architettura generale
- [CODING_GUIDELINES.md](../CODING_GUIDELINES.md) per linee guida di codifica
