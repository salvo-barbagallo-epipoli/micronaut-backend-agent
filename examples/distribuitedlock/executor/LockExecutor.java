package com.epipoli.starter.distribuitedlock.executor;

import com.epipoli.starter.exceptions.RequestAlreadyInProgressException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@Singleton
public class LockExecutor {

    private final Storage storage;
    private final String bucket;
    private final int ttl;
    private static final String LOCK_STRING = "_lock";

    public LockExecutor(GoogleCredentials googleCredentials,
                        @Value("${locks.bucket}") String bucket,
                        @Value("${locks.ttl}") Integer ttl) {
        this.storage = StorageOptions.newBuilder().setCredentials(googleCredentials).build().getService();
        this.bucket = bucket;
        this.ttl = ttl;
    }

    public <T> Mono<T> withLock(String id, Supplier<Mono<T>> action) {
        AtomicBoolean lockAcquired = new AtomicBoolean(false);

        return acquire(id)
                .doOnSuccess(v -> lockAcquired.set(true))
                .then(Mono.defer(action))
                .doFinally(signal -> {
                    if (lockAcquired.get()) {
                        release(id);
                    }
                });
    }

    private Mono<Void> acquire(String id) {
        return Mono.fromRunnable(() -> {
            Blob blob = storage.get(bucket, id);

            if (blob != null) {
                String expiresAtStr = blob.getMetadata() != null ? blob.getMetadata().get("expiresAt") : null;
                if (expiresAtStr != null) {
                    long expiresAt = Long.parseLong(expiresAtStr);
                    long now = System.currentTimeMillis();
                    if (now <= expiresAt) {
                        throw new RequestAlreadyInProgressException();
                    }
                    storage.delete(blob.getBlobId());
                } else {
                    throw new RequestAlreadyInProgressException();
                }
            }

            long expiresAt = System.currentTimeMillis() + (ttl * 1000);
            BlobInfo blobInfo = BlobInfo.newBuilder(bucket, id)
                    .setMetadata(Map.of("expiresAt", String.valueOf(expiresAt)))
                    .build();
            storage.create(blobInfo, LOCK_STRING.getBytes(), Storage.BlobTargetOption.doesNotExist());
        });
    }

    private void release(String id) {
        BlobId blobId = BlobInfo.newBuilder(bucket, id).build().getBlobId();
        Blob blob = storage.get(blobId);
        if (blob != null) {
            storage.delete(blobId);
        }
    }
}