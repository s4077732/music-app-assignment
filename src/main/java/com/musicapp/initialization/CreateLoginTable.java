package com.musicapp.initialization;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Program to create the login table in DynamoDB (AWS SDK v2)
 * This connects to ONLINE AWS (not local)
 */
public class CreateLoginTable {

    public static void main(String[] args) throws Exception {

        // Create DynamoDB client for ONLINE AWS
        DynamoDbClient dynamoDb = DynamoDbClient.builder()
                .region(software.amazon.awssdk.regions.Region.US_EAST_1)
                .build();

        String tableName = "login";
        String studentId = "s4113122";   // your student id
        String userBaseName = "TijuAugustine"; // your name

        try {
            System.out.println("Creating table: " + tableName);

            // Create table request
            CreateTableRequest createTableRequest = CreateTableRequest.builder()
                    .tableName(tableName)
                    .keySchema(
                            KeySchemaElement.builder()
                                    .attributeName("email")
                                    .keyType(KeyType.HASH)  // Partition key
                                    .build()
                    )
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("email")
                                    .attributeType(ScalarAttributeType.S)  // String
                                    .build()
                    )
                    .billingMode(BillingMode.PAY_PER_REQUEST)  // On-demand pricing
                    .build();

            try {
                CreateTableResponse response = dynamoDb.createTable(createTableRequest);
                System.out.println("✓ Table created successfully!");
                System.out.println("  Table ARN: " + response.tableDescription().tableArn());
            } catch (ResourceInUseException e) {
                System.out.println("✓ Table already exists. Skipping creation.");
            }

            // Wait for table to be active
            waitForTableActive(dynamoDb, tableName);

            // Insert 10 records
            insertSampleUsers(dynamoDb, tableName, studentId, userBaseName);

            System.out.println("\n✓ Login table setup complete!");

        } finally {
            dynamoDb.close();
        }
    }

    /**
     * Wait for table to become ACTIVE before inserting data
     */
    private static void waitForTableActive(DynamoDbClient dynamoDb, String tableName)
            throws InterruptedException {
        System.out.println("Waiting for table to be ACTIVE...");

        boolean isActive = false;
        int attempts = 0;

        while (!isActive && attempts < 60) {
            DescribeTableRequest describeRequest = DescribeTableRequest.builder()
                    .tableName(tableName)
                    .build();

            DescribeTableResponse response = dynamoDb.describeTable(describeRequest);
            TableStatus status = response.table().tableStatus();

            if (status == TableStatus.ACTIVE) {
                isActive = true;
                System.out.println("✓ Table is now ACTIVE!");
            } else {
                System.out.println("  Current status: " + status);
                Thread.sleep(1000);
                attempts++;
            }
        }

        if (!isActive) {
            throw new RuntimeException("Table did not become active within 60 seconds");
        }
    }

    /**
     * Insert 10 sample users into the login table
     */
    private static void insertSampleUsers(DynamoDbClient dynamoDb, String tableName,
                                          String studentId, String userBaseName) {
        System.out.println("\nInserting 10 sample users...");

        for (int i = 0; i < 10; i++) {
            String email = studentId + i + "@student.rmit.edu.au";
            String userName = userBaseName + i;

            // Generate password: 012345 → 901234
            String password = "" + i + ((i + 1) % 10) + ((i + 2) % 10)
                    + ((i + 3) % 10) + ((i + 4) % 10) + ((i + 5) % 10);

            // Create item with attributes
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("email", AttributeValue.builder().s(email).build());
            item.put("user_name", AttributeValue.builder().s(userName).build());
            item.put("password", AttributeValue.builder().s(password).build());

            // Put item into table
            PutItemRequest putRequest = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build();

            try {
                dynamoDb.putItem(putRequest);
                System.out.println("  ✓ Inserted: " + email + " | " + userName + " | " + password);
            } catch (DynamoDbException e) {
                System.err.println("  ✗ Error inserting " + email + ": " + e.getMessage());
            }
        }

        System.out.println("✓ All 10 users inserted successfully!");
    }
}