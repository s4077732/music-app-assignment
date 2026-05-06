package com.musicapp.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Generic API Response DTO
 * Used for all API responses
 *
 * Response Body:
 * {
 *   "success": true,
 *   "message": "Operation successful",
 *   "data": {...}
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

}