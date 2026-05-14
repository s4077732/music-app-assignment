package com.amazonaws.samples;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;

public class GetUserSubscriptions {

    public static void main(String[] args) {

        // Test value.
        String email = "s40777320@student.rmit.edu.au";

        List<Item> subscriptions = getSubscriptions(email);

        if (subscriptions.isEmpty()) {
            System.out.println("No subscriptions yet.");
        } else {
            // Print each subscribed song returned from DynamoDB.
            for (Item item : subscriptions) {
                System.out.println("-------------------");
                System.out.println("Title: " + item.getString("title"));
                System.out.println("Artist: " + item.getString("artist"));
                System.out.println("Year: " + item.getString("year"));
                System.out.println("Album: " + item.getString("album"));
                System.out.println("Image URL: " + item.getString("image_url"));
            }
        }
    }

    /*
     * Retrieve all songs subscribed by a specific user.
     * The email is used as the partition key, so DynamoDB can efficiently
     * return all subscription records belonging to that user.
     */
    public static List<Item> getSubscriptions(String email) {

        List<Item> subscriptions = new ArrayList<>();

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);

        // Access the subscription table that stores user-song subscription records.
        Table table = dynamoDB.getTable("subscription");

        /*
         * Query the subscription table using the user's email.
         * This returns all songs subscribed by that user because email is the partition key.
         */
        QuerySpec querySpec = new QuerySpec()
                .withKeyConditionExpression("email = :email")
                .withValueMap(new ValueMap().withString(":email", email));

        try {
            ItemCollection<QueryOutcome> items = table.query(querySpec);

            // Add each returned DynamoDB item to the subscription list.
            for (Item item : items) {
                subscriptions.add(item);
            }

        } catch (Exception e) {
            // Print an error if DynamoDB cannot return the user's subscriptions.
            System.err.println("Unable to get subscriptions:");
            System.err.println(e.getMessage());
        }

        return subscriptions;
    }
}