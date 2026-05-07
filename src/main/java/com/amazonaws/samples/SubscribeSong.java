package com.amazonaws.samples;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;

public class SubscribeSong {

    public static void main(String[] args) {
        // Test only
        subscribeSong(
                "s40777320@student.rmit.edu.au",
                "Taylor Swift",
                "Love Story",
                "2008",
                "Fearless",
                "https://music-application-img-upload.s3.amazonaws.com/Taylor_Swift.jpg"
        );
    }

    // ✅ THIS is what UI/API will use
    public static void subscribeSong(String email, String artist, String title,
                                     String year, String album, String imageUrl) {

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable("subscription");

        // unique id (important!)
        String songId = title + "_" + year;

        // Check if already exists
        Item existingItem = table.getItem("email", email, "song_id", songId);

        if (existingItem != null) {
            System.out.println("Already subscribed!");
            return;
        }

        Item item = new Item()
                .withPrimaryKey("email", email, "song_id", songId)
                .withString("artist", artist)
                .withString("title", title)
                .withString("year", year)
                .withString("album", album)
                .withString("image_url", imageUrl);

        try {
            table.putItem(item);
            System.out.println("Subscription added successfully.");
        } catch (Exception e) {
            System.err.println("Unable to subscribe:");
            System.err.println(e.getMessage());
        }
    }
}