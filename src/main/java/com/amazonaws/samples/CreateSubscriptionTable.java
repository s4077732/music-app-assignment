package com.amazonaws.samples;

import java.util.Arrays;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.*;

public class CreateSubscriptionTable {

    public static void main(String[] args) throws Exception {

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();


        DynamoDB dynamoDB = new DynamoDB(client);
        String tableName = "subscription";

        try {
            /*
             * Create the subscription table.
             * This table stores the songs subscribed by each user.
             */
            Table table = dynamoDB.createTable(
                    tableName,

                    /*
                     * Primary key design:
                     * - email is the partition key, so all subscriptions for one user
                     *   can be retrieved efficiently.
                     * - song_id is the sort key, so each user can subscribe to multiple songs
                     *   without overwriting previous subscriptions.
                     */
                    Arrays.asList(
                            new KeySchemaElement("email", KeyType.HASH),
                            new KeySchemaElement("song_id", KeyType.RANGE)
                    ),
                    Arrays.asList(
                            new AttributeDefinition("email", ScalarAttributeType.S),
                            new AttributeDefinition("song_id", ScalarAttributeType.S)
                    ),
                    // Set the read and write capacity for the table.
                    new ProvisionedThroughput(10L, 10L)
            );

            // Wait until the table is fully created before using it.
            table.waitForActive();
            System.out.println("Subscription table created: " + table.getDescription().getTableStatus());

        } catch (ResourceInUseException e) {
            // If the table already exists, the program continues without creating it again.
            System.out.println("Subscription table already exists.");
        }
    }
}