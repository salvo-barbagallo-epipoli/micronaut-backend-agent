package com.epipoli.starter;

import com.epipoli.commons.helper.ListResponse;
import io.micronaut.runtime.Micronaut;
import io.micronaut.serde.annotation.SerdeImport;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.info.*;


@OpenAPIDefinition(
    info = @Info(
            title = "starter",
            version = "0.1"
    )
)
@SerdeImport(ListResponse.class)
public class Application {

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}