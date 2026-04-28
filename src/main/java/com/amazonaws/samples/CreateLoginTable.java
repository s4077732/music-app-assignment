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

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);

        String tableName = "login";
        String studentId = "s4077732";   // your student id
        String userBaseName = "NithyaJ"; // your name

        try {
            System.out.println("Creating table...");

            // Create table
            Table table = dynamoDB.createTable(
                    tableName,
                    Arrays.asList(new KeySchemaElement("email", KeyType.HASH)), // Partition key
                    Arrays.asList(new AttributeDefinition("email", ScalarAttributeType.S)),
                    new ProvisionedThroughput(10L, 10L)
            );

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
            System.err.println("Unable to create table: ");
            System.err.println(e.getMessage());
        }
    }
}