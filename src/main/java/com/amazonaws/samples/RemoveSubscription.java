package com.amazonaws.samples;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;

public class RemoveSubscription {

    public static void main(String[] args) {

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable("subscription");

        // Test values. Later these come from logged-in user + Remove button.
        String email = "s40777320@student.rmit.edu.au";
        String songId = "Jimmy Buffett#Margaritaville#1974";

        try {
            DeleteItemSpec deleteSpec = new DeleteItemSpec()
                    .withPrimaryKey("email", email, "song_id", songId);

            table.deleteItem(deleteSpec);

            System.out.println("Subscription removed successfully.");

        } catch (Exception e) {
            System.out.println("Unable to remove subscription.");
            System.out.println(e.getMessage());
        }
    }
}