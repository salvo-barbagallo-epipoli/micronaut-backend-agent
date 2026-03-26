package com.epipoli.starter.graal;

import com.epipoli.commons.helper.ListResponse;
import io.micronaut.core.annotation.TypeHint;
import io.micronaut.core.annotation.TypeHint.AccessType;
import net.logstash.logback.composite.loggingevent.LoggingEventJsonProviders;
import net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder;
import net.logstash.logback.encoder.LogstashEncoder;

@TypeHint(
        value = {
                ListResponse.class, LogstashEncoder.class, LoggingEventCompositeJsonEncoder.class, LoggingEventJsonProviders.class
        },
        accessType = {AccessType.ALL_DECLARED_FIELDS, AccessType.ALL_PUBLIC}
)
public class HighwaysTypeHint {

}