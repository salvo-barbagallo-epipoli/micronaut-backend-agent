package com.epipoli.starter.crud.filter;

import java.time.Instant;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@ReflectiveAccess
@Serdeable
@Data
public class OrderFilter {

    @Min(0)
    private Long customerId;

    @Size(min = 13, max = 13)
    private String productSku;

    @Schema(example = "2024-01-01T10:15:12.123Z")
    private Instant fromDate;

    @Schema(example = "2024-01-01T10:15:12.123Z")
    private Instant toDate;
}
