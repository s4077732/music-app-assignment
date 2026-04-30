package com.amazonaws.samples;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;

public class GetUserSubscriptions {

    public static void main(String[] args) {

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable("subscription");

        // Test logged-in user. Later this comes from session.
        String email = "s40777320@student.rmit.edu.au";

        QuerySpec querySpec = new QuerySpec()
                .withKeyConditionExpression("email = :email")
                .withValueMap(new ValueMap().withString(":email", email));

        ItemCollection<QueryOutcome> items = table.query(querySpec);

        int count = 0;

        for (Item item : items) {
            count++;

            System.out.println("-------------------");
            System.out.println("Title: " + item.getString("title"));
            System.out.println("Artist: " + item.getString("artist"));
            System.out.println("Year: " + item.getString("year"));
            System.out.println("Album: " + item.getString("album"));
            System.out.println("Image URL: " + item.getString("image_url"));
        }

        if (count == 0) {
            System.out.println("No subscriptions yet.");
        }
    }
}