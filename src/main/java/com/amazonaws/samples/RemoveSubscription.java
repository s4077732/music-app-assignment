package com.amazonaws.samples;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;

/**
 * This class handles removing a subscribed song
 * from the DynamoDB "subscription" table.
 */
public class RemoveSubscription {

    public static void main(String[] args) {

        // 🔹 Test values (later will come from UI)
        String email = "s40777320@student.rmit.edu.au";
        String songId = "Love Story_2008"; // must match how you created it in SubscribeSong

        removeSubscription(email, songId);
    }

    /**
     * 🔹 This method will be used by UI/API
     * Removes a subscription using email + song_id (primary key)
     */
    public static void removeSubscription(String email, String songId) {

        // 🔹 Create DynamoDB client
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        // 🔹 Connect to DynamoDB
        DynamoDB dynamoDB = new DynamoDB(client);

        // 🔹 Get the "subscription" table
        Table table = dynamoDB.getTable("subscription");

        try {
            // 🔹 Delete item using primary key
            // Partition key = email
            // Sort key = song_id
            table.deleteItem("email", email, "song_id", songId);

            System.out.println("Subscription removed successfully.");

        } catch (Exception e) {
            System.err.println("Unable to remove subscription:");
            System.err.println(e.getMessage());
        }
    }
}