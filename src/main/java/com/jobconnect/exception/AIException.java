package com.jobconnect.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AIException extends RuntimeException {

    private final HttpStatus status;

    public AIException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public AIException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}