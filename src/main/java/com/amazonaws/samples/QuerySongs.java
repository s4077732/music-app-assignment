package com.amazonaws.samples;

// Adapted from Practical Exercise 3 MoviesScan.java
// Modified for Assignment 2 music query functionality.

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;

public class QuerySongs {

    public static void main(String[] args) {

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable("music");

        // Test inputs. Later these values will come from UI text fields.
        String title = "";
        String year = "1974";
        String artist = "Jimmy Buffett";
        String album = "";

        if (title.isEmpty() && year.isEmpty() && artist.isEmpty() && album.isEmpty()) {
            System.out.println("Please enter at least one search field.");
            return;
        }

        StringBuilder filterExpression = new StringBuilder();
        NameMap nameMap = new NameMap();
        ValueMap valueMap = new ValueMap();

        if (!title.isEmpty()) {
            addAnd(filterExpression);
            filterExpression.append("#title = :title");
            nameMap.with("#title", "title");
            valueMap.withString(":title", title);
        }

        if (!year.isEmpty()) {
            addAnd(filterExpression);
            filterExpression.append("#year = :year");
            nameMap.with("#year", "year");
            valueMap.withNumber(":year", Integer.parseInt(year));
        }

        if (!artist.isEmpty()) {
            addAnd(filterExpression);
            filterExpression.append("#artist = :artist");
            nameMap.with("#artist", "artist");
            valueMap.withString(":artist", artist);
        }

        if (!album.isEmpty()) {
            addAnd(filterExpression);
            filterExpression.append("#album = :album");
            nameMap.with("#album", "album");
            valueMap.withString(":album", album);
        }

        ScanSpec scanSpec = new ScanSpec()
                .withFilterExpression(filterExpression.toString())
                .withNameMap(nameMap)
                .withValueMap(valueMap);

        try {
            ItemCollection<ScanOutcome> items = table.scan(scanSpec);

            int count = 0;

            for (Item item : items) {
                count++;

                System.out.println("----------------------------");
                System.out.println("Title: " + item.getString("title"));
                System.out.println("Artist: " + item.getString("artist"));
                System.out.println("Year: " + item.getNumber("year"));
                System.out.println("Album: " + item.getString("album"));
                System.out.println("Image URL: " + item.getString("image_url"));
            }

            if (count == 0) {
                System.out.println("No result is retrieved. Please query again");
            }

        } catch (Exception e) {
            System.err.println("Unable to query songs:");
            System.err.println(e.getMessage());
        }
    }

    private static void addAnd(StringBuilder filterExpression) {
        if (filterExpression.length() > 0) {
            filterExpression.append(" AND ");
        }
    }
}