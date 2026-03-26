package com.epipoli.starter.exceptions;


import com.epipoli.commons.exceptions.HighwaysUniqueException;
import io.micronaut.core.convert.exceptions.ConversionErrorException;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.server.exceptions.InternalServerException;
import io.micronaut.security.authentication.AuthorizationException;
import jakarta.validation.ConstraintViolationException;

@Controller
public class ErrorController {

    /**
     * CUSTOM EXCEPTION - DemoException
     * @param request
     * @param ex
     * @return
     */
    @Error(global = true)
    public HttpResponse<ErrorMessage> handleDemoException(HttpRequest<?> request, DemoException exception) {
        return HttpResponse.status(HttpStatus.valueOf(exception.getHttpStatus()))
                .body(new ErrorMessage(
                        exception.getCode(),
                        exception.getMessage()
                ));
    }




    @Error(global = true)
    public HttpResponse<ErrorMessage> handleUniqueException(HttpRequest<?> request, HighwaysUniqueException exception) {
        return HttpResponse.status(HttpStatus.CONFLICT)
                .body(new ErrorMessage(
                        HttpStatus.CONFLICT.getCode(),
                        exception.getMessage()
                ));
    }


    @Error(global = true)
    public HttpResponse<ErrorMessage> handleConstraintViolationException(HttpRequest<?> request, ConstraintViolationException exception) {
        return HttpResponse.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorMessage(
                        HttpStatus.BAD_REQUEST.getCode(),
                        exception.getMessage()
                ));
    }


    @Error(global = true)
    public HttpResponse<ErrorMessage> handleAuthorizationException(HttpRequest<?> request, AuthorizationException exception) {
        return HttpResponse.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorMessage(
                        HttpStatus.UNAUTHORIZED.getCode(),
                        exception.getMessage()
                ));
    }

    @Error(global = true)
    public HttpResponse<ErrorMessage> handleValidation(HttpRequest<?> request, ConstraintViolationException ex) {
        return HttpResponse.badRequest(new ErrorMessage(4000000, ex.getMessage()));
    }

    @Error(global = true)
    public HttpResponse<ErrorMessage> handleValidation(HttpRequest<?> request, ConversionErrorException ex) {
        return HttpResponse.badRequest(new ErrorMessage(4000000, ex.getMessage()));
    }
    
    @Error(global = true)
    public HttpResponse<ErrorMessage> handleIllegalArgument(HttpRequest<?> request, IllegalArgumentException ex) {
        return HttpResponse.badRequest(new ErrorMessage(4000000, ex.getMessage()));
    }

    @Error(global = true)
    public HttpResponse<ErrorMessage> handleHttpClientError(HttpRequest<?> request, HttpClientResponseException ex) {
        return HttpResponse.badRequest(new ErrorMessage(4000000, ex.getMessage()));
    }

    @Error(global = true)
    public HttpResponse<ErrorMessage> handleInternalServer(HttpRequest<?> request, InternalServerException ex) {
        return HttpResponse.badRequest(new ErrorMessage(5000000, ex.getMessage()));
    }


    

    /**
     * OTHERS
     * @param request
     * @param exception
     * @return
     */

    @Error(global = true)
    public HttpResponse<ErrorMessage> handleThrowableError(HttpRequest<?> request, Throwable exception) {
        exception.printStackTrace();
        return HttpResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorMessage(
                        HttpStatus.SERVICE_UNAVAILABLE.getCode(),
                        exception.getMessage()
                ));
    }

}