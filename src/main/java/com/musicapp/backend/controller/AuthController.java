package com.musicapp.backend.controller;

import com.musicapp.backend.model.ApiResponse;
import com.musicapp.backend.model.LoginRequest;
import com.musicapp.backend.model.RegisterRequest;
import com.musicapp.backend.model.User;
import com.musicapp.backend.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/login
     *
     * Request body:
     * {
     *   "email": "user@student.rmit.edu.au",
     *   "password": "123456"
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<User>> login(@RequestBody LoginRequest request) {

        if (request.getEmail() == null || request.getEmail().trim().isEmpty()
                || request.getPassword() == null || request.getPassword().trim().isEmpty()) {

            return ResponseEntity.badRequest().body(
                    ApiResponse.<User>builder()
                            .success(false)
                            .message("Email and password are required")
                            .data(null)
                            .build()
            );
        }

        Optional<User> user = authService.login(request.getEmail(), request.getPassword());

        if (user.isPresent()) {
            return ResponseEntity.ok(
                    ApiResponse.<User>builder()
                            .success(true)
                            .message("Login successful")
                            .data(user.get())
                            .build()
            );
        }

        return ResponseEntity.status(401).body(
                ApiResponse.<User>builder()
                        .success(false)
                        .message("Invalid email or password")
                        .data(null)
                        .build()
        );
    }

    /**
     * POST /api/register
     *
     * Request body:
     * {
     *   "email": "newuser@student.rmit.edu.au",
     *   "user_name": "New User",
     *   "password": "123456"
     * }
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@RequestBody RegisterRequest request) {

        if (request.getEmail() == null || request.getEmail().trim().isEmpty()
                || request.getUser_name() == null || request.getUser_name().trim().isEmpty()
                || request.getPassword() == null || request.getPassword().trim().isEmpty()) {

            return ResponseEntity.badRequest().body(
                    ApiResponse.<User>builder()
                            .success(false)
                            .message("Email, username and password are required")
                            .data(null)
                            .build()
            );
        }

        User newUser = new User();
        newUser.setEmail(request.getEmail());
        newUser.setUser_name(request.getUser_name());
        newUser.setPassword(request.getPassword());

        boolean registered = authService.register(newUser);

        if (registered) {
            return ResponseEntity.ok(
                    ApiResponse.<User>builder()
                            .success(true)
                            .message("Registration successful")
                            .data(newUser)
                            .build()
            );
        }

        return ResponseEntity.badRequest().body(
                ApiResponse.<User>builder()
                        .success(false)
                        .message("Email already exists or registration failed")
                        .data(null)
                        .build()
        );
    }
}