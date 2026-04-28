package com.amazonaws.samples;

import java.io.File;
import java.util.Iterator;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MusicLoadData {

    public static void main(String[] args) throws Exception {

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable("music");

        // Put 2026a2_songs.json in the project root,
        // or change this path to match your folder structure.
        File jsonFile = new File("2026a2_songs.json");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonFile);
        JsonNode songsNode = rootNode.get("songs");

        if (songsNode == null || !songsNode.isArray()) {
            System.err.println("Invalid JSON format: 'songs' array not found.");
            return;
        }

        Iterator<JsonNode> iter = songsNode.iterator();
        ObjectNode currentNode;
        int insertedCount = 0;

        while (iter.hasNext()) {
            currentNode = (ObjectNode) iter.next();

            String title = currentNode.path("title").asText();
            String artist = currentNode.path("artist").asText();
            int year = currentNode.path("year").asInt();
            String album = currentNode.path("album").asText();

            // Dataset field is img_url, but assignment asks for image_url
            String imageUrl = currentNode.path("img_url").asText();

            // Helper sort key to avoid accidental overwriting
            String titleYear = title + "_" + year;

            try {
                Item item = new Item()
                        .withPrimaryKey("artist", artist, "title_year", titleYear)
                        .withString("title", title)
                        .withString("artist", artist)
                        .withNumber("year", year)
                        .withString("album", album)
                        .withString("image_url", imageUrl);

                table.putItem(item);
                insertedCount++;

                System.out.println("Inserted: " + artist + " | " + title + " | " + year);

            } catch (Exception e) {
                System.err.println("Unable to insert song: " + artist + " | " + title + " | " + year);
                System.err.println(e.getMessage());
            }
        }

        System.out.println("Data loading complete. Total inserted: " + insertedCount);
    }
}