package com.epipoli.starter.distribuitedlock.executor;

import com.epipoli.starter.exceptions.RequestAlreadyInProgressException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import java.util.Map;
import java.util.function.Supplier;

@Singleton
public class BlockingLockExecutor {

    private final Storage storage;
    private final String bucket;
    private final int ttl;
    private static final String LOCK_STRING = "_lock";

    public BlockingLockExecutor(GoogleCredentials googleCredentials,
                                @Value("${locks.bucket}") String bucket,
                                @Value("${locks.ttl}") Integer ttl) {
        this.storage = StorageOptions.newBuilder().setCredentials(googleCredentials).build().getService();
        this.bucket = bucket;
        this.ttl = ttl;
    }

    public <T> T withLock(String id, Supplier<T> action) {
        boolean lockAcquired = false;

        try {
            acquire(id);
            lockAcquired = true;
            return action.get();
        } finally {
            if (lockAcquired) {
                release(id);
            }
        }
    }

    private void acquire(String id) {
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
    }

    private void release(String id) {
        BlobId blobId = BlobInfo.newBuilder(bucket, id).build().getBlobId();
        Blob blob = storage.get(blobId);
        if (blob != null) {
            storage.delete(blobId);
        }
    }
}