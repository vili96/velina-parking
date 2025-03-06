package com.example.parking.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

import static com.example.parking.util.Constants.ERROR_STATUS;
import static com.example.parking.util.Constants.SUCCESS_STATUS;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private final String status;
    private final T data;
    private final ErrorInfo error;
    private final LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(SUCCESS_STATUS, data, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(String message, int code) {
        return new ApiResponse<>(ERROR_STATUS, null, new ErrorInfo(message, code), LocalDateTime.now());
    }

    public static <T> ApiResponse<T> validationError(String message, int code, Map<String, String> fieldErrors) {
        return new ApiResponse<>(
                ERROR_STATUS,
                null,
                new ErrorInfo(message, code, fieldErrors),
                LocalDateTime.now()
        );
    }

    @Getter
    @AllArgsConstructor
    public static class ErrorInfo {
        private final String message;
        private final int code;
        private final Map<String, String> fields;

        public ErrorInfo(String message, int code) {
            this(message, code, null);
        }
    }
}
