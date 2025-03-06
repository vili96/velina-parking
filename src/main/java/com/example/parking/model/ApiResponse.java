package com.example.parking.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private final String status;
    private final T data;
    private final ErrorInfo error;
    private final LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", data, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(String message, int code) {
        return new ApiResponse<>("error", null, new ErrorInfo(message, code), LocalDateTime.now());
    }

    public static <T> ApiResponse<T> validationError(String message, int code, Map<String, String> fieldErrors) {
        return new ApiResponse<>(
                "error",
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
