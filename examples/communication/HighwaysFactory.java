package com.epipoli.starter.factory;

import com.epipoli.communication.sender.CommunicationSender;
import com.epipoli.communication.sender.CommunicationSenderConfig;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.annotation.Bean;


@Factory
public class HighwaysFactory {

    private final CommunicationSenderConfig communicationConfig;

    public HighwaysFactory(

            //Communication
            @Value("${highways.communication.data.projectId:pd}") String communicationProjectId,
            @Value("${highways.communication.data.datastoreId:}") String communicationDatastoreId,
            @Value("${highways.communication.data.namespace:default}") String communicationNamespace,
            @Value("${highways.communication.template.projectId:pd}") String communicationTemplateProjectId,
            @Value("${highways.communication.template.datastoreId:}") String communicationTemplateDatastoreId,
            @Value("${highways.communication.template.namespace:default}") String communicationTemplateNamespace,
            @Value("${highways.communication.topic.projectId}") String communicationTopicProjectId,
            @Value("${highways.communication.topic.id}") String communicationTopicId
            ) {

        this.communicationConfig =  CommunicationSenderConfig.builder()
                .templateDatastoreProjectId(communicationTemplateDatastoreId)
                .templateDatastoreId(communicationTemplateDatastoreId)
                .templateNamespace(communicationTemplateNamespace)
                .datastoreProjectId(communicationProjectId)
                .datastoreId(communicationDatastoreId)
                .namespace(communicationNamespace)
                .topicProjectId(communicationTopicProjectId)
                .topicId(communicationTopicId)
                .build();
    }

    @Bean
    public CommunicationSender communicationSender() {
        return new CommunicationSender(communicationConfig);
    }
}