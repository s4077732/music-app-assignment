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

        // Test value. Later this will come from logged-in user session.
        String email = "s40777320@student.rmit.edu.au";

        List<Item> subscriptions = getSubscriptions(email);

        if (subscriptions.isEmpty()) {
            System.out.println("No subscriptions yet.");
        } else {
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

    // This method can be called by UI/API later.
    public static List<Item> getSubscriptions(String email) {

        List<Item> subscriptions = new ArrayList<>();

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable("subscription");

        QuerySpec querySpec = new QuerySpec()
                .withKeyConditionExpression("email = :email")
                .withValueMap(new ValueMap().withString(":email", email));

        try {
            ItemCollection<QueryOutcome> items = table.query(querySpec);

            for (Item item : items) {
                subscriptions.add(item);
            }

        } catch (Exception e) {
            System.err.println("Unable to get subscriptions:");
            System.err.println(e.getMessage());
        }

        return subscriptions;
    }
}