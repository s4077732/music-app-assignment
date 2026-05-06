package com.musicapp.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Register Request DTO - Received from frontend
 *
 * Request Body:
 * {
 *   "email": "User@email.com",
 *   "user_name": "username",
 *   "password": "password123"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String email;
    private String user_name;
    private String password;
}