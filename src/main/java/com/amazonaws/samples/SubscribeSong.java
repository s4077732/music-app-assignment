package com.amazonaws.samples;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;

public class SubscribeSong {

    public static void main(String[] args) {

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                "http://localhost:8000",
                                Regions.US_EAST_1.getName()))
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable("subscription");

        String email = "s40777320@student.rmit.edu.au";
        String artist = "Taylor Swift";
        String title = "Love Story";
        String year = "2008";
        String album = "Fearless";
        String imageUrl = "https://music-application-img-upload.s3.amazonaws.com/Taylor_Swift.jpg";

        String songId = artist + "#" + title + "#" + year;

        Item item = new Item()
                .withPrimaryKey("email", email, "song_id", songId)
                .withString("artist", artist)
                .withString("title", title)
                .withString("year", year)
                .withString("album", album)
                .withString("image_url", imageUrl);

        table.putItem(item);

        System.out.println("Song subscribed successfully.");
    }
}