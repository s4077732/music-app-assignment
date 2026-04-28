package com.amazonaws.samples;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;

public class Userlogin {

    public static void main(String[] args) {

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);
        Table loginTable = dynamoDB.getTable("login");

        // Test values. Later these will come from UI input fields.
        String email = "s40777320@student.rmit.edu.au";
        String password = "012345";

        Item user = loginTable.getItem("email", email);

        if (user == null) {
            System.out.println("email or password is invalid");
            return;
        }

        String storedPassword = user.getString("password");

        if (storedPassword.equals(password)) {
            System.out.println("Login successful");
            System.out.println("User name: " + user.getString("user_name"));
        } else {
            System.out.println("email or password is invalid");
        }
    }
}