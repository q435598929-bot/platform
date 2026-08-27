package com.platform.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.authentication.BadCredentialsException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(BadCredentialsException.class)
    ProblemDetail unauthorized(BadCredentialsException e) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        detail.setDetail(e.getMessage());
        return detail;
    }
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail notFound(IllegalArgumentException e) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        detail.setDetail(e.getMessage());
        return detail;
    }
    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail conflict(IllegalStateException e) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        detail.setDetail(e.getMessage());
        return detail;
    }
}
