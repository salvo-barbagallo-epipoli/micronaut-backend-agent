package com.epipoli.starter.crud.service;

import com.epipoli.commons.interfaces.IListResponse;
import com.epipoli.commons.queryfilter.HwCompositeFilter;
import com.epipoli.commons.queryfilter.HwFilters;
import com.epipoli.commons.queryfilter.HwQueryOptions;
import com.epipoli.commons.queryfilter.HwSortDirection;
import com.epipoli.commons.repository.HwEntityService;
import com.epipoli.starter.crud.filter.ProductFilter;
import com.epipoli.starter.crud.model.Product;
import com.epipoli.starter.crud.request.NewProductRequest;
import com.epipoli.starter.crud.request.PaginationRequest;
import com.epipoli.starter.crud.request.UpdateProductRequest;
import com.epipoli.starter.crud.response.ProductResponseTiny;
import com.epipoli.starter.exceptions.ProductNotFoundException;
import jakarta.inject.Singleton;


@Singleton
public class ProductService {
    
    private final HwEntityService hwEntityService;

    public ProductService(HwEntityService hwEntityService){
        this.hwEntityService = hwEntityService;
    }

    public ProductResponseTiny createProduct(NewProductRequest product){
        product.setId(product.getSku());
        return hwEntityService.create(product, ProductResponseTiny.class);
    }

    public void updateProduct(String productSku, UpdateProductRequest product){

        //Check
        hwEntityService.findById(Product.class, productSku).orElseThrow(ProductNotFoundException::new);

        product.setId(productSku);
        hwEntityService.upsert(product);
    }

    public Product productDetail(String productSku){
        return hwEntityService.findById(Product.class, productSku).orElseThrow(ProductNotFoundException::new);
    }

    public IListResponse<Product> listProducts(ProductFilter productFilter, PaginationRequest pagination){

        HwCompositeFilter filter = HwFilters.and(
            HwFilters.eq("sku", productFilter.getSku()),
            HwFilters.gte("price", productFilter.getFromPrice()),
            HwFilters.lte("price", productFilter.getToPrice())
        );

        HwQueryOptions options = HwQueryOptions.builder()
        .start(pagination.getStart())
        .limit(pagination.getLimit())
        .includeCount(true)
        .build();

        return hwEntityService.list(Product.class, filter, options);
    }
    

    public IListResponse<ProductResponseTiny> listProductsTiny(ProductFilter productFilter, PaginationRequest pagination){

        HwCompositeFilter filter = HwFilters.and(
            HwFilters.eq("sku", productFilter.getSku()),
            HwFilters.gte("price", productFilter.getFromPrice()),
            HwFilters.lte("price", productFilter.getToPrice())
        );

        HwQueryOptions options = HwQueryOptions.builder()
        .start(pagination.getStart())
        .limit(pagination.getLimit())
        .addSort("sku", HwSortDirection.ASC)
        .includeCount(true)
        .build();

        return hwEntityService.list(Product.class, filter, options, ProductResponseTiny.class);
    }

}
