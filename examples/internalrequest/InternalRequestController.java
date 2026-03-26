package com.epipoli.starter.internalrequest;

import java.io.IOException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.inject.Singleton;


@Singleton
public class InternalRequestController {

    private final GoogleCredentials googleCredentials;
    private final HttpClient httpClient;

    public InternalRequestController(GoogleCredentials googleCredentials, HttpClient httpClient){
        this.googleCredentials = googleCredentials;
        this.httpClient = httpClient;
    }


    @ExecuteOn(TaskExecutors.BLOCKING) 
    public String intRequest() throws IOException {
        String endpoint = "https://contentprovider-dummy-42386345137.europe-west1.run.app/health";
        String accessToken = this.getAccessTokenCredentials(endpoint);

        HttpRequest<?> request = HttpRequest.GET(endpoint)
        .bearerAuth(accessToken)
        .accept(MediaType.APPLICATION_JSON);

        try {
            return httpClient.toBlocking().retrieve(request, String.class);
        } catch (HttpClientResponseException e) {
            // Log the error or wrap it into a custom exception
            System.err.println("Errore HTTP: " + e.getStatus() + " - " + e.getMessage());
            throw e; // Or return null / default value
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            throw e;
        }
    }



    private String getAccessTokenCredentials(String targetAudience) throws IOException {

        if (!(googleCredentials instanceof IdTokenProvider)) {
            throw new RuntimeException("Credentials are not an instance of IdTokenProvider.");
        }
        IdTokenCredentials idTokenCredentials = IdTokenCredentials.newBuilder()
        .setIdTokenProvider((IdTokenProvider) googleCredentials)
        .setTargetAudience(targetAudience.replaceAll("/+$", ""))
        .build();
    
        return idTokenCredentials.refreshAccessToken().getTokenValue();
    }
    
}





