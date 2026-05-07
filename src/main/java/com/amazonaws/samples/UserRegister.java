package com.amazonaws.samples;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;

/**
 * This class handles user registration.
 * It checks whether the email already exists in the DynamoDB "login" table.
 * If the email is unique, it stores the new user details.
 */
public class UserRegister {

    public static void main(String[] args) {

        // Test values only. Later these will come from the UI register form.
        String email = "newuser@student.rmit.edu.au";
        String userName = "NewUser";
        String password = "123456";

        boolean registered = register(email, userName, password);

        if (registered) {
            System.out.println("User registered successfully!");
        } else {
            System.out.println("The email already exists");
        }
    }

    /**
     * This method can be called by UI/API later.
     *
     * @param email user-entered email
     * @param userName user-entered username
     * @param password user-entered password
     * @return true if registration is successful, false if email already exists or error occurs
     */
    public static boolean register(String email, String userName, String password) {

        // Create DynamoDB client for AWS region us-east-1
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        // Connect to DynamoDB
        DynamoDB dynamoDB = new DynamoDB(client);

        // Access the login table
        Table table = dynamoDB.getTable("login");

        try {
            // Check if email already exists
            Item existingUser = table.getItem("email", email);

            if (existingUser != null) {
                return false;
            }

            // Create new user item
            Item newUser = new Item()
                    .withPrimaryKey("email", email)
                    .withString("user_name", userName)
                    .withString("password", password);

            // Store new user in login table
            table.putItem(newUser);

            return true;

        } catch (Exception e) {
            System.err.println("Unable to register user:");
            System.err.println(e.getMessage());
            return false;
        }
    }
}