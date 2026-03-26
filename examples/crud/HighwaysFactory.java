package com.epipoli.starter.factory;

import com.epipoli.commons.datastore.DatastoreConfig;
import com.epipoli.commons.datastore.DatastoreRequestExecutor;
import com.epipoli.commons.helper.CrudService;
import com.epipoli.commons.repository.HwEntityService;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.annotation.Bean;
import java.io.File;
import java.io.FileInputStream;

@Factory
public class HighwaysFactory {

    private final GoogleCredentials googleCredentials;
    private final Datastore datastore;
    private final String namespace;
    private final String secureSecretPrefix;
    private final String secureSecretKey;

    public HighwaysFactory(
            //Database
            @Value("${highways.datastore.projectId:pd}") String projectId,
            @Value("${highways.datastore.datastoreId:}") String datastoreId,
            @Value("${highways.datastore.namespace:default}") String namespace,
            @Value("${highways.datastore.secureSecretPrefix:-}") String secureSecretPrefix,
            @Value("${highways.datastore.secureSecretKey:-}") String secureSecretKey,

            ) {
        this.googleCredentials = loadGoogleCredentials();
        this.datastore = createDatastoreService(projectId, datastoreId, googleCredentials);
        this.namespace = namespace;
        this.secureSecretPrefix = secureSecretPrefix;
        this.secureSecretKey = secureSecretKey;

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
    public CrudService crudService() {
        DatastoreConfig config = DatastoreConfig.builder()
                .datastoreInstance(datastore)
                .namespace(namespace)
                .secureSecretPrefix(secureSecretPrefix)
                .secureSecretKey(secureSecretKey)
                .build();

        return new CrudService(new DatastoreRequestExecutor(config));
    }

    @Bean
    public HwEntityService entityService() {
        return new HwEntityService(crudService());
    }

    @Bean
    private GoogleCredentials loadGoogleCredentials() {
        String credentialsPath = System.getenv().getOrDefault("GCLOUD_CREDENTIALS", "/sa/serviceaccount.json");
        try {
            File credentialsFile = new File(credentialsPath);
            if (credentialsFile.exists()) {
                return GoogleCredentials.fromStream(new FileInputStream(credentialsFile))
                        .createScoped("https://www.googleapis.com/auth/cloud-platform");
            }
            return GoogleCredentials.getApplicationDefault();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load GoogleCredentials", e);
        }
    }

    private Datastore createDatastoreService(String projectId, String datastoreId, GoogleCredentials credentials) {
        DatastoreOptions.Builder builder = DatastoreOptions.newBuilder()
                .setCredentials(credentials)
                .setProjectId(projectId);

        if (datastoreId != null && !datastoreId.isBlank()) {
            builder.setDatabaseId(datastoreId);
        }

        return builder.build().getService();
    }

}