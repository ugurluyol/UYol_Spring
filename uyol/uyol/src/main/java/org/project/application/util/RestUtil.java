package org.project.application.util;

import org.project.application.dto.common.ErrorMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;

public final class RestUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private RestUtil() {
    }

    public static ResponseStatusException responseException(HttpStatus status, String message) {
        try {
            // keep JSON serialization behavior like before
            objectMapper.writeValueAsString(new ErrorMessage(message));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unexpected exception, error message can’t be serialized.");
        }

        return new ResponseStatusException(status, message);
    }

    public static <T> T required(String fieldName, T field) {
        if (field == null) {
            throw responseException(
                    HttpStatus.BAD_REQUEST,
                    "%s must be provided.".formatted(fieldName)
            );
        }
        return field;
    }

    public static ResponseStatusException unableToProcessRequestException() {
        return responseException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unable to process your request at the moment. Please try again."
        );
    }
}
