package com.epipoli.starter.graal;

import io.micronaut.core.annotation.TypeHint;

@TypeHint(
    value = {
        net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder.class,
        net.logstash.logback.composite.loggingevent.LoggingEventJsonProviders.class,
        net.logstash.logback.composite.loggingevent.ArgumentsJsonProvider.class,
        net.logstash.logback.composite.loggingevent.MessageJsonProvider.class,
        net.logstash.logback.composite.loggingevent.LoggerNameJsonProvider.class,
        net.logstash.logback.composite.loggingevent.MdcJsonProvider.class,
        net.logstash.logback.composite.loggingevent.StackTraceJsonProvider.class,
        net.logstash.logback.composite.loggingevent.LoggingEventFormattedTimestampJsonProvider.class,
    },
    accessType = {TypeHint.AccessType.ALL_DECLARED_CONSTRUCTORS, TypeHint.AccessType.ALL_DECLARED_FIELDS}
)
public class LogbackJsonTypeHints {
}
