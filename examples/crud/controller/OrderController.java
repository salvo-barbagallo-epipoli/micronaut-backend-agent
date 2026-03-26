package com.epipoli.starter.crud.controller;

import com.epipoli.commons.interfaces.IListResponse;
import com.epipoli.starter.crud.filter.OrderFilter;
import com.epipoli.starter.crud.model.Order;
import com.epipoli.starter.crud.request.PaginationRequest;
import com.epipoli.starter.crud.response.FullOrderListResponse;
import com.epipoli.starter.crud.service.OrderService;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.validation.Valid;

@Controller("demo/order")
@ExecuteOn(TaskExecutors.BLOCKING)
public class OrderController {

    private final OrderService orderService;
   

    public OrderController(OrderService orderService){
        this.orderService = orderService;
    
    }

    @Post
    public Order create(@Valid @Body Order order){
        return orderService.createOrder(order);
    }

    @Post("trx")
    public Order createWithTrx(@Valid @Body Order order){
        return orderService.createOrderTrx(order);
    }

    @Post("trx-event")
    public Order createOrderTrxWithEvent(@Valid @Body Order order){
        return orderService.createOrderTrxWithEvent(order);
    }

    @Get("{?filter*,pagination*}")
    public IListResponse<Order> list(@Valid OrderFilter filter, PaginationRequest pagination){
        return orderService.listOrders(filter, pagination);
    }

    @Get("full{?filter*,pagination*}")
    public IListResponse<FullOrderListResponse> fullList(@Valid OrderFilter filter, PaginationRequest pagination){
        return orderService.listOrdersFull(filter, pagination);
    }
    
}
