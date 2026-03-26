package com.epipoli.starter.crud.controller;

import com.epipoli.commons.interfaces.IListResponse;
import com.epipoli.starter.crud.filter.ProductFilter;
import com.epipoli.starter.crud.model.Product;
import com.epipoli.starter.crud.request.NewProductRequest;
import com.epipoli.starter.crud.request.PaginationRequest;
import com.epipoli.starter.crud.request.UpdateProductRequest;
import com.epipoli.starter.crud.response.ProductResponseTiny;
import com.epipoli.starter.crud.service.ProductService;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.validation.Valid;

@Controller("demo/product")
@ExecuteOn(TaskExecutors.BLOCKING)
public class ProductController {

    private final ProductService productService;
   

    public ProductController(ProductService productService){
        this.productService = productService;
    }

    @Post
    public ProductResponseTiny create(@Valid @Body NewProductRequest product){
        return productService.createProduct(product);
    }

    @Put("{productSku}")
    public void update(String productSku, @Valid @Body UpdateProductRequest updateProduct){
        productService.updateProduct(productSku, updateProduct);
    }

    @Get("{productSku}")
    public Product details(String productSku){
        return productService.productDetail(productSku);
    }

    @Get("{?filter*,pagination*}")
    public IListResponse<Product> listProducts(@Valid ProductFilter filter, PaginationRequest pagination){
        return productService.listProducts(filter, pagination);
    }

    @Get("product-tiny{?filter*,pagination*}")
    public IListResponse<ProductResponseTiny> listProductsTiny(@Valid ProductFilter filter, PaginationRequest pagination){
        return productService.listProductsTiny(filter, pagination);
    }

    
}
