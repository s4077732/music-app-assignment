package com.amazonaws.samples;

// Adapted from Practical Exercise 3 MoviesScan.java
// Modified for Assignment 2 music query functionality.

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;

public class QuerySongs {

    public static void main(String[] args) {

        // Test inputs. Later these values will come from UI text fields.
        String title = "";
        String year = "1974";
        String artist = "Jimmy Buffett";
        String album = "";

        List<Item> results = querySongs(title, year, artist, album);

        if (results.isEmpty()) {
            System.out.println("No result is retrieved. Please query again");
        } else {
            for (Item item : results) {
                System.out.println("----------------------------");
                System.out.println("Title: " + item.getString("title"));
                System.out.println("Artist: " + item.getString("artist"));
                System.out.println("Year: " + item.getNumber("year"));
                System.out.println("Album: " + item.getString("album"));
                System.out.println("Image URL: " + item.getString("image_url"));
            }
        }
    }

    // This method can be called by UI/API later.
    public static List<Item> querySongs(String title, String year, String artist, String album) {

        List<Item> results = new ArrayList<>();

        if (isEmpty(title) && isEmpty(year) && isEmpty(artist) && isEmpty(album)) {
            System.out.println("Please enter at least one search field.");
            return results;
        }

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable("music");

        StringBuilder filterExpression = new StringBuilder();
        NameMap nameMap = new NameMap();
        ValueMap valueMap = new ValueMap();

        if (!isEmpty(title)) {
            addAnd(filterExpression);
            filterExpression.append("#title = :title");
            nameMap.with("#title", "title");
            valueMap.withString(":title", title.trim());
        }

        if (!isEmpty(year)) {
            addAnd(filterExpression);
            filterExpression.append("#year = :year");
            nameMap.with("#year", "year");
            valueMap.withNumber(":year", Integer.parseInt(year.trim()));
        }

        if (!isEmpty(artist)) {
            addAnd(filterExpression);
            filterExpression.append("#artist = :artist");
            nameMap.with("#artist", "artist");
            valueMap.withString(":artist", artist.trim());
        }

        if (!isEmpty(album)) {
            addAnd(filterExpression);
            filterExpression.append("#album = :album");
            nameMap.with("#album", "album");
            valueMap.withString(":album", album.trim());
        }

        ScanSpec scanSpec = new ScanSpec()
                .withFilterExpression(filterExpression.toString())
                .withNameMap(nameMap)
                .withValueMap(valueMap);

        try {
            ItemCollection<ScanOutcome> items = table.scan(scanSpec);

            for (Item item : items) {
                results.add(item);
            }

        } catch (Exception e) {
            System.err.println("Unable to query songs:");
            System.err.println(e.getMessage());
        }

        return results;
    }

    private static void addAnd(StringBuilder filterExpression) {
        if (filterExpression.length() > 0) {
            filterExpression.append(" AND ");
        }
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}