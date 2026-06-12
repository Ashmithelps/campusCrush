package com.example.campuscrush.error;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class LimitExceededHandler {

    @ExceptionHandler(LimitExceededException.class)
    public ResponseEntity<Map<String, String>> handle(LimitExceededException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("code", e.getCode()));
    }
}
