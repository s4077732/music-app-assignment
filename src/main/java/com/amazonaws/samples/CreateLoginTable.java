package com.amazonaws.samples;

import java.util.Arrays;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.*;

public class CreateLoginTable {

    public static void main(String[] args) throws Exception {

        // Create a DynamoDB client in the us-east-1 region.
        // This client is used to communicate with the DynamoDB service in AWS.
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);

        String tableName = "login";
        // Base values used to generate the 10 sample login users required for the assignment.
        String studentId = "s4077732";
        String userBaseName = "NithyaJ";

        try {
            System.out.println("Creating table...");

            /*
             * Create the login table with email as the partition key.
             * The email is used as the unique identifier because each user must register
             * with a unique email address.
             */
            Table table = dynamoDB.createTable(
                    tableName,
                    Arrays.asList(new KeySchemaElement("email", KeyType.HASH)), // Partition key
                    Arrays.asList(new AttributeDefinition("email", ScalarAttributeType.S)),
                    new ProvisionedThroughput(10L, 10L)
            );

            // Wait until the table becomes active before inserting any data.
            table.waitForActive();
            System.out.println("Success.  Table status: " + table.getDescription().getTableStatus());

            // Insert 10 records using loop
            for (int i = 0; i < 10; i++) {

                String email = studentId + i + "@student.rmit.edu.au";
                String userName = userBaseName + i;

                // password pattern: 012345 → 901234
                String password = "" + i + ((i + 1) % 10) + ((i + 2) % 10)
                        + ((i + 3) % 10) + ((i + 4) % 10) + ((i + 5) % 10);

                Item item = new Item()
                        .withPrimaryKey("email", email)
                        .withString("user_name", userName)
                        .withString("password", password);

                table.putItem(item);

                System.out.println("Inserted: " + email + " | " + userName + " | " + password);
            }

            System.out.println("All 10 records inserted successfully!");

        } catch (Exception e) {
            // Print an error message if the table already exists or if DynamoDB rejects the request.
            System.err.println("Unable to create table: ");
            System.err.println(e.getMessage());
        }
    }
}