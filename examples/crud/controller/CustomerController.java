package com.epipoli.starter.crud.controller;

import com.epipoli.commons.interfaces.IListResponse;
import com.epipoli.starter.crud.filter.CustomerFilter;
import com.epipoli.starter.crud.model.Customer;
import com.epipoli.starter.crud.request.PaginationRequest;
import com.epipoli.starter.crud.service.CustomerService;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.validation.Valid;

@Controller("demo/customer")
@ExecuteOn(TaskExecutors.BLOCKING)
public class CustomerController {

    private final CustomerService customerService;
   

    public CustomerController(CustomerService customerService){
        this.customerService = customerService;
    
    }

    @Post
    public Customer create(@Valid @Body Customer customer){
        return customerService.createCustomer(customer);
    }

    @Put("{customerId}")
    public void update(Long customerId, @Valid @Body Customer updateCustomer){
        customerService.updateCustomer(customerId, updateCustomer);
    }

    @Get("{customerId}")
    public Customer list(Long customerId){
        return customerService.detail(customerId);
    }

    @Get("{?filter*,pagination*}")
    public IListResponse<Customer> list(@Valid CustomerFilter filter, PaginationRequest pagination){
        return customerService.listCustomers(filter, pagination);
    }
    
}
