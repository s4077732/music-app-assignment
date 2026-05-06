package com.musicapp.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User Model - Represents a User from DynamoDB login table
 *
 * Attributes from DynamoDB:
 * - email (Partition Key)
 * - user_name
 * - password
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String email;
    private String user_name;
    private String password;
}