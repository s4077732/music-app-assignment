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

/**
 * A one-time data initialization tool called MusicLoadData reads the
* imports all 137 songs into the DynamoDB using the 2026a2_songs.json dataset
* The "music" table.
* Before the application is launched, this program is executed once to populate
* The catalog of music. Once the table is online, it cannot be run again.
* PutItem will silently overwrite unless the table is emptied first.
* Any item that already exists and has the same composite primary key.
 * Key schema design rationale:
    * Prior to creating the key schema, the dataset was examined. Several songs
    * have the same title (for example, Taylor Swift's "Delicate" appears in several
    * albums and years), therefore overwrites would result from using the title as the only sort key.

    * Using a composite title_year string as the partition key and artist as the
    * The sort key ensures that each of the 137 records has a unique key, guaranteeing
    * An import of the entire dataset with 0% overwrite and perfect losslessness.

 * DynamoDB table: music
 * Partition key:  artist     (String)
 * Sort key:       title_year (String) — format: "title_year" e.g. "Love Story_2008"
 * Operation: PutItem for each song record
 */
public class MusicLoadData {

    /**
     * Reads and inserts each song from the 2026a2_songs.json music dataset.
    * into the DynamoDB music database as a separate item.*
    * PutItem is used to insert each song using the composite primary key.
    * (artist + title_year). The number of successful insertion is printed continuously.
    * at the conclusion so that the outcome can be compared to the anticipated total of 137.
    * Before executing, place 2026a2_songs.json in the project root directory.
* or change the file path to reflect your structure in the File constructor.
     *
     * @throws Exception if the JSON file cannot be read or parsed by Jackson.
     */
    public static void main(String[] args) throws Exception {

        /*
         * Build the DynamoDB client for us-east-1, the region where the music
         * table was created. Credentials are sourced automatically from the
         * local AWS CLI configuration or the EC2 instance's IAM role (LabRole),
         * so no access keys need to be hardcoded in the source code.
         */
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable("music");

        /*
         * Load the JSON dataset file from the project root directory.
         * ObjectMapper is the Jackson library's main entry point for reading JSON.
         * readTree() parses the entire file into a tree of JsonNode objects so
         * individual fields can be accessed by name without defining a Java class.
         */
        File jsonFile = new File("2026a2_songs.json");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode  = mapper.readTree(jsonFile);
        JsonNode songsNode = rootNode.get("songs");

        /*
         * Validate that the JSON file has the expected top-level "songs" array.
         * If the file is malformed or the field name has changed, exit early
         * with a clear error message rather than throwing a NullPointerException
         * deeper in the loop.
         */
        if (songsNode == null || !songsNode.isArray()) {
            System.err.println("Invalid JSON format: 'songs' array not found.");
            return;
        }

        Iterator<JsonNode> iter = songsNode.iterator();
        ObjectNode currentNode;
        int insertedCount = 0;

        while (iter.hasNext()) {
            currentNode = (ObjectNode) iter.next();

            /*
            * Take every attribute out of the JSON object for the current song.
             * Because path() returns, node.path() is utilized rather than node.get().* a MissingNode (not null) in the event that a field is missing, avoiding

             * NullPointerException in the event that any records in the dataset are incomplete.
             * Because year is stored as a number type in
* DynamoDB: By storing it as a number instead of a string, it allows
* the ArtistYearLSI for effective range queries on the year.
             */
            String title  = currentNode.path("title").asText();
            String artist = currentNode.path("artist").asText();
            int    year   = currentNode.path("year").asInt();
            String album  = currentNode.path("album").asText();

            /*
             * The field name "img_url" is used in the raw dataset, but the assignment
* the specification and the remainder of the program (MusicController, Lambda,
* SubscribeSong) all use "image_url" to refer to this attribute. The worth

             * is stored in DynamoDB under "image_url" after being read from "img_url" here.
* to guarantee system-wide name consistency.
             */
            String imageUrl = currentNode.path("img_url").asText();

            /*
            * Combine the year and title to create the composite sort key.
             * "title_year" is the format (e.g., "Love Story_2008", "Delicate_2017").* This ensures that each of the 137 records is unique, even when songs

             * have the same title, therefore no item is silently
             * Overwritten when being imported. The identical format is employed by
             * When creating song_id, use SubscribeSong so that subscriptions can be
             * reliably matched back to elements on the music table.
             */
            String titleYear = title + "_" + year;

            try {
                /*
                 * Build the DynamoDB Item with all required attributes.
                 * Primary key:
                 *   artist     — partition key, groups all songs by the same artist
                 *   title_year — sort key, uniquely identifies each song within an artist
                 * Additional attributes stored for display and indexing:
                 *   title     — stored separately from title_year for GSI-based title lookups
                 *   year      — stored as Number to support ArtistYearLSI range queries
                 *   album     — stored for display and AlbumArtistGSI-based album queries
                 *   image_url — S3 object key used by the backend to generate pre-signed URLs
                 */
                Item item = new Item()
                        .withPrimaryKey("artist", artist, "title_year", titleYear)
                        .withString("title",     title)
                        .withString("artist",    artist)
                        .withNumber("year",      year)
                        .withString("album",     album)
                        .withString("image_url", imageUrl);

                table.putItem(item);
                insertedCount++;

                System.out.println("Inserted: " + artist + " | " + title + " | " + year);

            } catch (Exception e) {
                /*
                * Continue processing the remaining songs after logging the unsuccessful insert.
                 * The entire load shouldn't be aborted by a single bad insert; the remaining* of the dataset can still be properly imported. Typical causes:

                 * A network timeout, an incorrect attribute value, or an exceeding throughput.
                 */
                System.err.println("Unable to insert song: " + artist + " | " + title + " | " + year);
                System.err.println(e.getMessage());
            }
        }

        /*
        * Print the final insert count so that it may be compared to the
* 137 songs are anticipated in all. If the count is less than 137, it means
* The error log needs to be examined since one or more songs did not insert.
         */
        System.out.println("Data loading complete. Total inserted: " + insertedCount);
    }
}