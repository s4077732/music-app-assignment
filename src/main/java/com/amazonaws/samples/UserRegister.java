package com.amazonaws.samples;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;

public class UserRegister {

    public static void main(String[] args) {

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable("login");

        // 🔴 Test input (later comes from UI)
        String email = "newuser@student.rmit.edu.au";
        String userName = "NewUser";
        String password = "123456";

        // 🔍 Check if email exists
        Item existingUser = table.getItem("email", email);

        if (existingUser != null) {
            System.out.println("The email already exists");
            return;
        }

        // ➕ Insert new user
        Item newUser = new Item()
                .withPrimaryKey("email", email)
                .withString("user_name", userName)
                .withString("password", password);

        table.putItem(newUser);

        System.out.println("User registered successfully!");
    }
}