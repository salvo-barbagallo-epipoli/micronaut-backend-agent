package com.epipoli.starter.distribuitedlock;

import java.time.Duration;

import com.epipoli.commons.repository.HwEntityService;
import com.epipoli.starter.crud.model.Product;
import com.epipoli.starter.distribuitedlock.executor.LockExecutor;
import com.epipoli.starter.exceptions.ProductNotFoundException;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import reactor.core.publisher.Mono;

@Controller("demo/lock")
@ExecuteOn(TaskExecutors.IO)
public class DemoLockReactiveController {

    private final LockExecutor lockExecutor;
    private final HwEntityService entityService;

    public DemoLockReactiveController(LockExecutor lockExecutor, HwEntityService entityService) {
        this.lockExecutor = lockExecutor;
        this.entityService = entityService;
    }

    @Get("reactive/{sku}")
    public Mono<Product> reactiveLock(String sku) {

        String lockId = String.format("%s_lock", sku);

        return lockExecutor.withLock(lockId, () -> Mono.delay(Duration.ofSeconds(30)) //Simulate 30s delay
                .flatMap(tick -> Mono.fromCallable(() -> {
                    Product product = entityService.findById(Product.class, sku).orElseThrow(ProductNotFoundException::new);
                    return product;
                }   
        )));
    }

}
