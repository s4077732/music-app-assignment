package com.musicapp.initialization;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * ASSIGNMENT REQUIREMENT 3: Write a program to load the data from 2026a2_songs.json
 * to the music table.
 *
 * Key Considerations:
 * - 136 total songs in the JSON file
 * - Some songs have duplicate titles but different artists (e.g., "Bad Blood")
 * - Some artists have multiple songs (e.g., Taylor Swift has 7 songs)
 * - Some songs appear in multiple album versions (e.g., "Delicate" by Taylor Swift)
 *
 * To prevent data loss (no overwrites), we use composite songId:
 * songId = title + "#" + album + "#" + year
 *
 * This ensures each combination is unique and no data is lost.
 */
public class LoadData {

    public static void main(String[] args) throws Exception {

        DynamoDbClient dynamoDb = DynamoDbClient.builder()
                .region(software.amazon.awssdk.regions.Region.US_EAST_1)
                .build();

        String tableName = "music";
        String jsonFilePath = "2026a2_songs.json";

        try {
            System.out.println("==== Loading Songs from JSON ====\n");

            // Step 1: Read JSON file
            System.out.println("Reading JSON file: " + jsonFilePath);
            String jsonContent = readJsonFile(jsonFilePath);

            // Step 2: Parse JSON
            System.out.println("Parsing JSON data...\n");
            JsonArray songsArray = parseSongsFromJson(jsonContent);

            // Step 3: Load songs into DynamoDB
            System.out.println("Loading songs into DynamoDB table: " + tableName + "\n");
            loadSongsIntoDynamoDB(dynamoDb, tableName, songsArray);

            System.out.println("\n✓ ALL SONGS LOADED SUCCESSFULLY!");
            System.out.println("  - Total songs: " + songsArray.size());

        } catch (Exception e) {
            System.err.println("✗ ERROR: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dynamoDb.close();
        }
    }

    /**
     * Read the JSON file from disk
     */
    private static String readJsonFile(String filePath) throws Exception {
        try {
            byte[] content = Files.readAllBytes(Paths.get(filePath));
            return new String(content);
        } catch (Exception e) {
            System.err.println("✗ Error reading file: " + filePath);
            throw new Exception("Cannot read JSON file. Please ensure 2026a2_songs.json is in the correct path.", e);
        }
    }

    /**
     * Parse JSON and extract songs array
     *
     * Expected JSON structure:
     * {
     *   "songs": [
     *     {
     *       "title": "1904",
     *       "artist": "The Tallest Man on Earth",
     *       "year": "2012",
     *       "album": "There's No Leaving Now",
     *       "img_url": "https://..."
     *     },
     *     ...
     *   ]
     * }
     */
    private static JsonArray parseSongsFromJson(String jsonContent) throws Exception {
        try {
            JsonElement jsonElement = JsonParser.parseString(jsonContent);
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            JsonArray songsArray = jsonObject.getAsJsonArray("songs");

            if (songsArray == null) {
                throw new Exception("JSON does not contain 'songs' array");
            }

            System.out.println("✓ Found " + songsArray.size() + " songs in JSON file");
            return songsArray;

        } catch (Exception e) {
            System.err.println("✗ Error parsing JSON: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Load all songs into DynamoDB
     *
     * For each song:
     * - artist (PK)
     * - songId (SK) = title#album#year (ensures uniqueness)
     * - title
     * - album
     * - year
     * - img_url (image_url from JSON)
     */
    private static void loadSongsIntoDynamoDB(DynamoDbClient dynamoDb, String tableName,
                                              JsonArray songsArray) {
        int successCount = 0;
        int errorCount = 0;

        for (int i = 0; i < songsArray.size(); i++) {
            try {
                JsonObject songJson = songsArray.get(i).getAsJsonObject();

                // Extract data from JSON
                String title = songJson.get("title").getAsString();
                String artist = songJson.get("artist").getAsString();
                String year = songJson.get("year").getAsString();
                String album = songJson.get("album").getAsString();
                String imgUrl = songJson.get("img_url").getAsString();

                // Create composite songId: title#album#year
                // This ensures uniqueness and prevents overwrites
                String songId = title + "#" + album + "#" + year;

                // Create DynamoDB item
                Map<String, AttributeValue> item = new HashMap<>();
                item.put("artist", AttributeValue.builder().s(artist).build());
                item.put("songId", AttributeValue.builder().s(songId).build());
                item.put("title", AttributeValue.builder().s(title).build());
                item.put("album", AttributeValue.builder().s(album).build());
                item.put("year", AttributeValue.builder().s(year).build());
                item.put("img_url", AttributeValue.builder().s(imgUrl).build());

                // Put item into DynamoDB
                PutItemRequest putRequest = PutItemRequest.builder()
                        .tableName(tableName)
                        .item(item)
                        .build();

                dynamoDb.putItem(putRequest);

                successCount++;

                // Print progress every 10 songs
                if ((i + 1) % 10 == 0) {
                    System.out.println("  ✓ Loaded " + (i + 1) + "/" + songsArray.size() + " songs");
                }

            } catch (Exception e) {
                errorCount++;
                System.err.println("  ✗ Error loading song " + (i + 1) + ": " + e.getMessage());
            }
        }

        System.out.println("\n✓ Load complete!");
        System.out.println("  - Successfully loaded: " + successCount);
        System.out.println("  - Errors: " + errorCount);
    }
}