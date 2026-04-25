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
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                "http://localhost:8000",
                                Regions.US_EAST_1.getName()))
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);
        String tableName = "subscription";

        try {
            Table table = dynamoDB.createTable(
                    tableName,
                    Arrays.asList(
                            new KeySchemaElement("email", KeyType.HASH),
                            new KeySchemaElement("song_id", KeyType.RANGE)
                    ),
                    Arrays.asList(
                            new AttributeDefinition("email", ScalarAttributeType.S),
                            new AttributeDefinition("song_id", ScalarAttributeType.S)
                    ),
                    new ProvisionedThroughput(10L, 10L)
            );

            table.waitForActive();
            System.out.println("Subscription table created: " + table.getDescription().getTableStatus());

        } catch (ResourceInUseException e) {
            System.out.println("Subscription table already exists.");
        }
    }
}