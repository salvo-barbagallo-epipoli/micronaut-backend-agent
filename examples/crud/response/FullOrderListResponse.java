package com.epipoli.starter.crud.response;

import java.time.Instant;
import com.epipoli.commons.annotation.HWAttribute;
import com.epipoli.commons.annotation.HWEntity;
import com.epipoli.commons.interfaces.IEntity;
import com.epipoli.starter.crud.model.Customer;
import com.epipoli.starter.crud.model.HwEntityName;
import com.epipoli.starter.crud.model.Product;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

@Serdeable 
@Data
@ReflectiveAccess
@HWEntity(HwEntityName.ORDER)
public class FullOrderListResponse implements IEntity<Long> {

    @HWAttribute
    private Long id;

    @HWAttribute
    private Instant orderDate;

    @HWAttribute
    private Double total;

    // Linking only the ID has no join cost in NoSQL systems like Datastore
    @HWAttribute(links = {HwEntityName.CUSTOMER}) // No join cost
    private Long customerId;

    // Expensive in LIST operations: fetching the full linked object requires additional queries.
    // Acceptable in detail views where a single object is fetched.
    @HWAttribute(links = {HwEntityName.CUSTOMER}) // Costly in LIST context
    private Customer customer;

    // Same as above: join is costly when listing many items
    @HWAttribute(links = {HwEntityName.PRODUCT}) // Costly in LIST context
    private Product product;

    // Even partial field joins are expensive in LISTs, since they require fetching the related entity
    @HWAttribute(links = {HwEntityName.PRODUCT}, linkFields = {"name"}) // Costly in LIST context
    private String productName;

}
