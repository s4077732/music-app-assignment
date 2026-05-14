package com.amazonaws.samples;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.LocalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;

public class CreateMusicTable {

    public static void main(String[] args) throws Exception {

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();
        DynamoDB dynamoDB = new DynamoDB(client);
        String tableName = "music";

        try {
            System.out.println("Creating table '" + tableName + "'...");

            /*
             * Create the music table.
             * The key schema is designed to avoid overwriting songs when loading
             * data from the JSON file.
             */
            CreateTableRequest request = new CreateTableRequest()
                    .withTableName(tableName)

                    // Primary key:
                    // artist = partition key
                    // title_year = sort key
                    // title_year is a helper attribute used to prevent overwriting
                    .withKeySchema(
                            new KeySchemaElement("artist", KeyType.HASH),
                            new KeySchemaElement("title_year", KeyType.RANGE)
                    )

                    // Only key/index attributes go here
                    .withAttributeDefinitions(
                            new AttributeDefinition("artist", ScalarAttributeType.S),
                            new AttributeDefinition("title_year", ScalarAttributeType.S),
                            new AttributeDefinition("year", ScalarAttributeType.N),
                            new AttributeDefinition("title", ScalarAttributeType.S),
                            new AttributeDefinition("album", ScalarAttributeType.S)
                    )

                    .withProvisionedThroughput(new ProvisionedThroughput(10L, 10L))

                    // LSI: query songs for an artist and sort/filter by year
                    .withLocalSecondaryIndexes(
                            new LocalSecondaryIndex()
                                    .withIndexName("ArtistYearLSI")
                                    .withKeySchema(
                                            new KeySchemaElement("artist", KeyType.HASH),
                                            new KeySchemaElement("year", KeyType.RANGE)
                                    )
                                    .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
                    )

                    // GSI 1: query by title
                    .withGlobalSecondaryIndexes(
                            new GlobalSecondaryIndex()
                                    .withIndexName("TitleArtistGSI")
                                    .withKeySchema(
                                            new KeySchemaElement("title", KeyType.HASH),
                                            new KeySchemaElement("artist", KeyType.RANGE)
                                    )
                                    .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
                                    .withProvisionedThroughput(new ProvisionedThroughput(10L, 10L)),

                            // GSI 2: query by album
                            new GlobalSecondaryIndex()
                                    .withIndexName("AlbumArtistGSI")
                                    .withKeySchema(
                                            new KeySchemaElement("album", KeyType.HASH),
                                            new KeySchemaElement("artist", KeyType.RANGE)
                                    )
                                    .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
                                    .withProvisionedThroughput(new ProvisionedThroughput(10L, 10L))
                    );
            // Create the table and wait until DynamoDB marks it as active.
            Table table = dynamoDB.createTable(request);
            table.waitForActive();

            System.out.println("Success. Table status: " + table.getDescription().getTableStatus());

        } catch (ResourceInUseException e) {
            // This prevents the program from failing if the table has already been created.
            System.out.println("Table '" + tableName + "' already exists.");
        } catch (Exception e) {
            // Print the error details if the table creation fails for another reason.
            System.err.println("Unable to create table:");
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
}