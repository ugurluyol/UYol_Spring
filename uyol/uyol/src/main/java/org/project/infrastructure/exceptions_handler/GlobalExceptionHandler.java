package org.project.infrastructure.exceptions_handler;

import org.project.application.dto.common.ErrorMessage;
import org.project.domain.shared.exceptions.DomainException;
import org.project.domain.user.exceptions.BannedUserException;
import com.hadzhy.jetquerious.exceptions.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BannedUserException.class)
    public ResponseEntity<ErrorMessage> handleBannedUser(BannedUserException e) {
        log.error("Banned user error", e);
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorMessage(e.getMessage()));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorMessage> handleDomainException(DomainException e) {
        log.error("Domain error", e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorMessage(e.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Void> handleNotFound(NotFoundException e) {
        log.error("Not found", e);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .build();
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorMessage> handleGeneric(Throwable e) {
        log.error("Unexpected error", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorMessage(
                        "Unexpected error occurred. Please contact support."
                ));
    }
}
