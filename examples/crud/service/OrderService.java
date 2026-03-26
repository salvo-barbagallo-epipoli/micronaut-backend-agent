package com.epipoli.starter.crud.service;

import java.time.Instant;
import com.epipoli.commons.interfaces.IListResponse;
import com.epipoli.commons.interfaces.ITransactionManager;
import com.epipoli.commons.queryfilter.HwCompositeFilter;
import com.epipoli.commons.queryfilter.HwFilters;
import com.epipoli.commons.queryfilter.HwQueryOptions;
import com.epipoli.commons.queryfilter.HwSortDirection;
import com.epipoli.commons.repository.HwEntityService;
import com.epipoli.starter.crud.filter.OrderFilter;
import com.epipoli.starter.crud.model.Customer;
import com.epipoli.starter.crud.model.Order;
import com.epipoli.starter.crud.model.Product;
import com.epipoli.starter.crud.request.PaginationRequest;
import com.epipoli.starter.crud.response.FullOrderListResponse;
import com.epipoli.starter.exceptions.CustomerNotFoundException;
import com.epipoli.starter.exceptions.ProductNotFoundException;
import jakarta.inject.Singleton;


@Singleton
public class OrderService {
    
    private final HwEntityService hwEntityService;

    public OrderService(HwEntityService hwEntityService){
        this.hwEntityService = hwEntityService;
    }



    public Order createOrder(Order order){
    
        Instant now = Instant.now();

        //check Customer
        Customer customer = hwEntityService.findById(Customer.class, order.getCustomerId()).orElseThrow(CustomerNotFoundException::new);

        //check Product
        Product product = hwEntityService.findById(Product.class, order.getProductId()).orElseThrow(ProductNotFoundException::new);

        order.setId(null); //Id Autogenerato
        order.setOrderDate(now);
        order.setTotal(product.getPrice());

        //Update customer
        customer.setLastOrderDate(now);
        hwEntityService.upsert(customer);

        return hwEntityService.create(order);
    }


    public Order createOrderTrx(Order order){

        ITransactionManager trx = hwEntityService.transactionHandler();
        trx.start();
    
        Instant now = Instant.now();

        //check Customer
        Customer customer = hwEntityService.findById(Customer.class, order.getCustomerId()).orElseThrow(CustomerNotFoundException::new);

        //check Product
        Product product = hwEntityService.findById(Product.class, order.getProductId()).orElseThrow(ProductNotFoundException::new);

        order.setId(null); //Id Autogenerato
        order.setOrderDate(now);
        order.setTotal(product.getPrice());

        //Update customer
        customer.setLastOrderDate(now);
        trx.upsert(customer);

        //Create Order
        trx.create(order);

        trx.commit();

        return order;
    }


    public Order createOrderTrxWithEvent(Order order){

        ITransactionManager trx = hwEntityService.transactionHandler();

        trx.startWithEvent("NEW_ORDER", "API", "webapp", "webapp");
    
        Instant now = Instant.now();

        //check Customer
        Customer customer = hwEntityService.findById(Customer.class, order.getCustomerId()).orElseThrow(CustomerNotFoundException::new);

        //check Product
        Product product = hwEntityService.findById(Product.class, order.getProductId()).orElseThrow(ProductNotFoundException::new);

        order.setId(null); //Id Autogenerato
        order.setOrderDate(now);
        order.setTotal(product.getPrice());

        //Update customer
        customer.setLastOrderDate(now);
        trx.upsert(customer);

        //Create Order
        trx.create(order);

        trx.commit();

        return order;
    }


    public void updateOrder(Long id, Order order){
        order.setId(id); //Id
        hwEntityService.upsert(order);
    }

    public IListResponse<Order> listOrders(OrderFilter orderFilter, PaginationRequest pagination){

        HwCompositeFilter filter = HwFilters.and(
            HwFilters.eq("customerId", orderFilter.getCustomerId()),
            HwFilters.gte("orderDate", orderFilter.getFromDate()),
            HwFilters.lte("orderDate", orderFilter.getToDate())
        );

        HwQueryOptions options = HwQueryOptions.builder()
        .start(pagination.getStart())
        .limit(pagination.getLimit())
        .addSort("orderDate", HwSortDirection.DESC)
        .includeCount(true)
        .build();

        return hwEntityService.list(Order.class, filter, options);
    }
    

    public IListResponse<FullOrderListResponse> listOrdersFull(OrderFilter orderFilter, PaginationRequest pagination){

        HwCompositeFilter filter = HwFilters.and(
            HwFilters.eq("customerId", orderFilter.getCustomerId()),
            HwFilters.gte("orderDate", orderFilter.getFromDate()),
            HwFilters.lte("orderDate", orderFilter.getToDate())
        );

        HwQueryOptions options = HwQueryOptions.builder()
        .start(pagination.getStart())
        .limit(pagination.getLimit())
        .addSort("orderDate", HwSortDirection.DESC)
        .includeCount(true)
        .build();

        return hwEntityService.list(FullOrderListResponse.class, filter, options);
    }

}
