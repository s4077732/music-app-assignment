package com.amazonaws.samples;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;

/**
 * This class handles user login validation.
 * It checks whether the entered email and password match
 * the records stored in the DynamoDB "login" table.
 */
public class Userlogin {

    public static void main(String[] args) {

        // Test values only. Later these will come from the UI login form.
        String email = "s40777320@student.rmit.edu.au";
        String password = "012345";

        Item user = login(email, password);

        if (user != null) {
            System.out.println("Login successful");
            System.out.println("User name: " + user.getString("user_name"));
        } else {
            System.out.println("email or password is invalid");
        }
    }

    /**
     * This method can be called by UI/API later.
     *
     * @param email user-entered email
     * @param password user-entered password
     * @return user item if login is successful, otherwise null
     */
    public static Item login(String email, String password) {

        // Create DynamoDB client for AWS region us-east-1
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        // Connect to DynamoDB
        DynamoDB dynamoDB = new DynamoDB(client);

        // Access the login table
        Table table = dynamoDB.getTable("login");

        try {
            // Find user by email because email is the partition key
            Item user = table.getItem("email", email);

            // If email does not exist, login fails
            if (user == null) {
                return null;
            }

            // Get password stored in DynamoDB
            String storedPassword = user.getString("password");

            // Compare entered password with stored password
            if (storedPassword.equals(password)) {
                return user;
            }

        } catch (Exception e) {
            System.err.println("Unable to login:");
            System.err.println(e.getMessage());
        }

        // Return null if password is wrong or error occurs
        return null;
    }
}