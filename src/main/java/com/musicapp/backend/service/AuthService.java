package com.musicapp.backend.service;

import com.musicapp.backend.model.User;
import com.musicapp.backend.util.DynamoDbUtil;
import org.springframework.stereotype.Service;
import java.util.Optional;

/**
 * AuthService - Handle authentication operations
 *
 * Responsibilities:
 * - Login validation (check credentials in DynamoDB login table)
 * - User registration (create new User with unique email)
 * - User lookup
 *
 * Source: Custom implementation based on assignment requirements
 */
@Service
public class AuthService {

    private final DynamoDbUtil dynamoDbUtil;

    /**
     * Constructor injection of DynamoDbUtil
     */
    public AuthService(DynamoDbUtil dynamoDbUtil) {
        this.dynamoDbUtil = dynamoDbUtil;
    }

    /**
     * Login - Validate User credentials
     *
     * @param email User's email
     * @param password User's password
     * @return User object if valid, empty if invalid
     */
    public Optional<User> login(String email, String password) {
        try {
            // Query login table for User with this email
            Optional<User> user = dynamoDbUtil.getUserByEmail(email);

            if (user.isPresent()) {
                // Check if password matches
                if (user.get().getPassword().equals(password)) {
                    return user;  // Login successful
                }
            }

            // Invalid email or password
            return Optional.empty();

        } catch (Exception e) {
            System.err.println("Error during login: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Register - Create new User
     *
     * Validates:
     * 1. Email is not already registered
     * 2. All required fields are provided
     *
     * @param user New User to register
     * @return true if registration successful, false if email already exists
     */
    public boolean register(User user) {
        try {
            // Validate inputs
            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                System.err.println("Email is required");
                return false;
            }

            if (user.getUser_name() == null || user.getUser_name().trim().isEmpty()) {
                System.err.println("Username is required");
                return false;
            }

            if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                System.err.println("Password is required");
                return false;
            }

            // Check if email already exists
            Optional<User> existingUser = dynamoDbUtil.getUserByEmail(user.getEmail());

            if (existingUser.isPresent()) {
                System.out.println("Email already exists: " + user.getEmail());
                return false;  // Email already registered
            }

            // Email is unique, save new User
            dynamoDbUtil.saveUser(user);
            System.out.println("User registered successfully: " + user.getEmail());
            return true;

        } catch (Exception e) {
            System.err.println("Error during registration: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get User by email
     *
     * @param email User's email
     * @return User object if found, empty if not found
     */
    public Optional<User> getUserByEmail(String email) {
        try {
            return dynamoDbUtil.getUserByEmail(email);
        } catch (Exception e) {
            System.err.println("Error getting User: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }
}