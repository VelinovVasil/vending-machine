package com.vendingmachine.exception;

import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFound(ProductNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Product not found");
        problem.setProperty("errorCode", "PRODUCT_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(PurchaseDeclinedException.class)
    public ProblemDetail handlePurchaseDeclined(PurchaseDeclinedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.status(), exception.getMessage());
        problem.setTitle("Purchase declined");
        problem.setProperty("errorCode", exception.errorCode());
        problem.setProperty("returnedCoins", exception.returnedCoins());
        exception.properties().forEach(problem::setProperty);
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleRequestValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ProblemDetail problem = badRequest("Request body validation failed");
        problem.setProperty("errors", errors);
        problem.setProperty("errorCode", validationErrorCode(request));
        return problem;
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleMethodValidation(
            HandlerMethodValidationException exception,
            HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getParameterValidationResults().stream()
                .filter(ParameterErrors.class::isInstance)
                .map(ParameterErrors.class::cast)
                .flatMap(parameterErrors -> parameterErrors.getFieldErrors().stream())
                .forEach(fieldError -> errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage()));

        if (!errors.isEmpty()) {
            ProblemDetail problem = badRequest("Request body validation failed");
            problem.setProperty("errors", errors);
            problem.setProperty("errorCode", validationErrorCode(request));
            return problem;
        }

        ProblemDetail problem = badRequest("Request parameter validation failed");
        problem.setProperty("errorCode", validationErrorCode(request));
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        ProblemDetail problem = badRequest("Request parameter validation failed");
        problem.setProperty("errorCode", validationErrorCode(request));
        return problem;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ProblemDetail handleMalformedRequest(Exception exception, HttpServletRequest request) {
        ProblemDetail problem = badRequest("Request could not be parsed");
        problem.setProperty("errorCode", validationErrorCode(request));
        return problem;
    }

    private ProblemDetail badRequest(String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Invalid request");
        return problem;
    }

    private String validationErrorCode(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/vending/purchases")
                ? "INVALID_COIN_SELECTION"
                : "INVALID_REQUEST";
    }
}
