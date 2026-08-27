package com.platform.task.platform.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail notFound(IllegalArgumentException e) { return detail(HttpStatus.NOT_FOUND, e.getMessage()); }
    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail conflict(IllegalStateException e) { return detail(HttpStatus.CONFLICT, e.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalid(MethodArgumentNotValidException e) { return detail(HttpStatus.BAD_REQUEST, "Invalid task arguments"); }
    private ProblemDetail detail(HttpStatus status, String message) {
        ProblemDetail detail = ProblemDetail.forStatus(status); detail.setDetail(message); return detail;
    }
}
