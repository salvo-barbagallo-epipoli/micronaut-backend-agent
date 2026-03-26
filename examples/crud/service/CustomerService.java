package com.epipoli.starter.crud.service;

import com.epipoli.commons.interfaces.IListResponse;
import com.epipoli.commons.queryfilter.HwCompositeFilter;
import com.epipoli.commons.queryfilter.HwFilters;
import com.epipoli.commons.queryfilter.HwQueryOptions;
import com.epipoli.commons.repository.HwEntityService;
import com.epipoli.starter.crud.filter.CustomerFilter;
import com.epipoli.starter.crud.model.Customer;
import com.epipoli.starter.crud.request.PaginationRequest;
import com.epipoli.starter.exceptions.CustomerNotFoundException;

import jakarta.inject.Singleton;


@Singleton
public class CustomerService {
    
    private final HwEntityService hwEntityService;

    public CustomerService(HwEntityService hwEntityService){
        this.hwEntityService = hwEntityService;
    }


    public Customer createCustomer(Customer customer){
        customer.setId(null); //Id Autogenerato
        return hwEntityService.create(customer);
    }

    public void updateCustomer(Long id, Customer customer){
        customer.setId(id); //Id

        //Check
        hwEntityService.findById(Customer.class, id).orElseThrow(CustomerNotFoundException::new);

        hwEntityService.upsert(customer);
    }

    public Customer detail(Long id){
        return hwEntityService.findById(Customer.class, id).orElseThrow(CustomerNotFoundException::new);
    }

    public IListResponse<Customer> listCustomers(CustomerFilter customerFilter, PaginationRequest pagination){

        HwCompositeFilter filter = HwFilters.and(
            HwFilters.eq("email", customerFilter.getEmail())
        );

        HwQueryOptions options = HwQueryOptions.builder()
        .start(pagination.getStart())
        .limit(pagination.getLimit())
        .includeCount(true)
        .build();

        return hwEntityService.list(Customer.class, filter, options);
    }
    

}
