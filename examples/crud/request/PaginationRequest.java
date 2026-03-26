package com.epipoli.starter.crud.request;

import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ReflectiveAccess
@Serdeable
public class PaginationRequest {
    @QueryValue
    private Integer limit = 20;
    @QueryValue
    private Integer start = 0;
}