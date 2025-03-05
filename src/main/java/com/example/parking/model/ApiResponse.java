package com.example.parking.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Generic API response wrapper to standardize all API responses.
 * This provides a consistent structure for both success and error responses.
 *
 * @param <T> The type of data in the response
 */
@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private final String status;
    private final T data;
    private final ErrorInfo error;
    private final LocalDateTime timestamp;

    /**
     * Creates a successful response with data.
     *
     * @param data The data to include in the response
     * @param <T>  The type of data
     * @return A success response
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", data, null, LocalDateTime.now());
    }

    /**
     * Creates an error response.
     *
     * @param message The error message
     * @param code    The error code
     * @param <T>     The type of data (will be null for error responses)
     * @return An error response
     */
    public static <T> ApiResponse<T> error(String message, int code) {
        return new ApiResponse<>("error", null, new ErrorInfo(message, code), LocalDateTime.now());
    }

    /**
     * Creates a validation error response with field-specific errors.
     * <p>
     * Example:
     * ```
     * Map<String, String> fieldErrors = new HashMap<>();
     * fieldErrors.put("licensePlate", "License plate format is invalid");
     * fieldErrors.put("startTime", "Start time must be in the future");
     * ApiResponse.validationError("Validation failed", 400, fieldErrors);
     * ```
     *
     * @param message     The general error message
     * @param code        The error code
     * @param fieldErrors Map of field names to error messages
     * @param <T>         The type of data (will be null for error responses)
     * @return A validation error response
     */
    public static <T> ApiResponse<T> validationError(String message, int code, Map<String, String> fieldErrors) {
        return new ApiResponse<>(
                "error",
                null,
                new ErrorInfo(message, code, fieldErrors),
                LocalDateTime.now()
        );
    }

    /**
     * Nested class for error information.
     * <p>
     * The 'fields' map contains field-level validation errors where:
     * - Keys are field names (e.g., "licensePlate", "startTime")
     * - Values are error messages (e.g., "License plate format is invalid")
     */
    @Getter
    @AllArgsConstructor
    public static class ErrorInfo {
        private final String message;
        private final int code;
        private final Map<String, String> fields;

        /**
         * Constructor for simple errors without field-specific details.
         */
        public ErrorInfo(String message, int code) {
            this(message, code, null);
        }
    }
}
