package com.epipoli.starter.distribuitedlock;

import com.epipoli.commons.repository.HwEntityService;
import com.epipoli.starter.crud.model.Product;
import com.epipoli.starter.distribuitedlock.executor.BlockingLockExecutor;
import com.epipoli.starter.exceptions.ProductNotFoundException;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;

@Controller("demo/lock")
@ExecuteOn(TaskExecutors.BLOCKING)
public class DemoLockBlockingController {

    private final BlockingLockExecutor blockingLockExecutor;
    private final HwEntityService entityService;

    public DemoLockBlockingController(BlockingLockExecutor blockingLockExecutor, HwEntityService entityService) {
        this.entityService = entityService;
        this.blockingLockExecutor = blockingLockExecutor;
    }


    @Get("blocking/{sku}")
    public Product lock(String sku) {

        String lockId = String.format("%s_lock", sku);

        Product result = blockingLockExecutor.withLock(lockId, () -> {

            try{Thread.sleep(30000);}catch(Exception e){};

            return entityService.findById(Product.class, sku).orElseThrow(ProductNotFoundException::new);
        });

        return result;
    }

}
