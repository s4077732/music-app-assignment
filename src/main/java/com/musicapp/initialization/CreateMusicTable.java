package com.musicapp.initialization;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.util.Arrays;

/**
 * ASSIGNMENT REQUIREMENT 2: Write a program to create a table titled "music"
 *
 * Attributes:
 * - title (String)
 * - artist (String)
 * - year (String)
 * - album (String)
 * - image_url (String)
 *
 * Key Schema Design Analysis:
 * The JSON data contains 136 songs with potential duplicates:
 * - Same title, different artists (e.g., "Bad Blood")
 * - Same artist, multiple songs (e.g., Taylor Swift - 7 songs)
 * - Same song in different album versions (e.g., "Delicate" by Taylor Swift)
 *
 * To prevent data loss (no overwrites), we use composite keys:
 *
 * PRIMARY KEY DESIGN:
 * - Partition Key (HASH): artist
 *   Why: Most queries filter by artist first
 *
 * - Sort Key (RANGE): title#album#year
 *   Why: Ensures uniqueness, supports range queries
 *
 * INDEXES:
 * - GSI-1: Search by Title
 *   PK: title, SK: artist#year#album
 *
 * - LSI: Search by Artist + Year
 *   PK: artist, SK: year#title
 */
public class CreateMusicTable {

    public static void main(String[] args) throws Exception {

        DynamoDbClient dynamoDb = DynamoDbClient.builder()
                .region(software.amazon.awssdk.regions.Region.US_EAST_1)
                .build();

        String tableName = "music";

        try {
            System.out.println("==== Creating Music Table ====\n");

            createMusicTable(dynamoDb, tableName);
            waitForTableActive(dynamoDb, tableName);

            System.out.println("\n✓ MUSIC TABLE CREATED SUCCESSFULLY!");
            System.out.println("  - Table Name: " + tableName);
            System.out.println("  - Partition Key: artist");
            System.out.println("  - Sort Key: title#album#year");
            System.out.println("  - GSI-1 (Search by Title): title → artist#year#album");
            System.out.println("  - LSI (Search by Artist+Year): artist → year#title");

        } catch (Exception e) {
            System.err.println("✗ ERROR: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dynamoDb.close();
        }
    }

    /**
     * Creates the music table with optimized key schema
     *
     * Table Structure:
     * - Main Table: Partition by artist, sort by title#album#year
     * - GSI-1: Partition by title, sort by artist#year#album (for title searches)
     * - LSI: Partition by artist, sort by year#title (for year filters)
     */
    private static void createMusicTable(DynamoDbClient dynamoDb, String tableName) {
        System.out.println("Creating table: " + tableName);

        try {
            // Define main table key schema
            CreateTableRequest createTableRequest = CreateTableRequest.builder()
                    .tableName(tableName)

                    // PRIMARY KEY SCHEMA
                    .keySchema(
                            KeySchemaElement.builder()
                                    .attributeName("artist")
                                    .keyType(KeyType.HASH)  // Partition key
                                    .build(),
                            KeySchemaElement.builder()
                                    .attributeName("songId")  // Composite: title#album#year
                                    .keyType(KeyType.RANGE)  // Sort key
                                    .build()
                    )

                    // ATTRIBUTE DEFINITIONS
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("artist")
                                    .attributeType(ScalarAttributeType.S)
                                    .build(),
                            AttributeDefinition.builder()
                                    .attributeName("songId")
                                    .attributeType(ScalarAttributeType.S)
                                    .build(),
                            AttributeDefinition.builder()
                                    .attributeName("title")
                                    .attributeType(ScalarAttributeType.S)
                                    .build(),
                            AttributeDefinition.builder()
                                    .attributeName("year")
                                    .attributeType(ScalarAttributeType.S)
                                    .build()
                    )

                    // GLOBAL SECONDARY INDEX 1 - Search by Title
                    .globalSecondaryIndexes(
                            GlobalSecondaryIndex.builder()
                                    .indexName("title-artist-year-index")
                                    .keySchema(
                                            KeySchemaElement.builder()
                                                    .attributeName("title")
                                                    .keyType(KeyType.HASH)
                                                    .build(),
                                            KeySchemaElement.builder()
                                                    .attributeName("artist")
                                                    .keyType(KeyType.RANGE)
                                                    .build()
                                    )
                                    .projection(Projection.builder()
                                            .projectionType(ProjectionType.ALL)
                                            .build())
                                    .provisionedThroughput(ProvisionedThroughput.builder()
                                            .readCapacityUnits(5L)
                                            .writeCapacityUnits(5L)
                                            .build())
                                    .build()
                    )

                    // LOCAL SECONDARY INDEX - Search by Artist + Year
                    .localSecondaryIndexes(
                            LocalSecondaryIndex.builder()
                                    .indexName("artist-year-index")
                                    .keySchema(
                                            KeySchemaElement.builder()
                                                    .attributeName("artist")
                                                    .keyType(KeyType.HASH)
                                                    .build(),
                                            KeySchemaElement.builder()
                                                    .attributeName("year")
                                                    .keyType(KeyType.RANGE)
                                                    .build()
                                    )
                                    .projection(Projection.builder()
                                            .projectionType(ProjectionType.ALL)
                                            .build())
                                    .build()
                    )

                    // BILLING MODE
                    .billingMode(BillingMode.PROVISIONED)
                    .provisionedThroughput(ProvisionedThroughput.builder()
                            .readCapacityUnits(10L)
                            .writeCapacityUnits(10L)
                            .build())
                    .build();

            CreateTableResponse response = dynamoDb.createTable(createTableRequest);
            System.out.println("✓ Table created successfully!");
            System.out.println("  ARN: " + response.tableDescription().tableArn());

        } catch (ResourceInUseException e) {
            System.out.println("✓ Table already exists. Using existing table.");
        } catch (DynamoDbException e) {
            System.err.println("✗ Error creating table: " + e.awsErrorDetails().errorMessage());
            throw e;
        }
    }

    /**
     * Wait for table to be ACTIVE
     */
    private static void waitForTableActive(DynamoDbClient dynamoDb, String tableName)
            throws InterruptedException {
        System.out.println("\nWaiting for table to be ACTIVE...");

        boolean isActive = false;
        int attempts = 0;

        while (!isActive && attempts < 120) {
            DescribeTableRequest describeRequest = DescribeTableRequest.builder()
                    .tableName(tableName)
                    .build();

            try {
                DescribeTableResponse response = dynamoDb.describeTable(describeRequest);
                TableStatus status = response.table().tableStatus();

                if (status == TableStatus.ACTIVE) {
                    isActive = true;
                    System.out.println("✓ Table is now ACTIVE!");
                } else {
                    System.out.println("  Status: " + status);
                    Thread.sleep(1000);
                    attempts++;
                }
            } catch (ResourceNotFoundException e) {
                Thread.sleep(1000);
                attempts++;
            }
        }

        if (!isActive) {
            throw new RuntimeException("Table did not become active within 120 seconds");
        }
    }
}