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

/**
* The DynamoDB "music" table can be searched by one or more queries using QuerySongs.
 * Title, year, artist, and album are optional criteria.
 * At least one of the four parameters must be supplied; the other three are optional.
* AND logic is applied when more than one argument is provided.
* Only songs that match all of the fields are returned.
 *
* This class makes use of a dynamically constructed FilterExpression in a DynamoDB Scan.
 * A scan applies the filter after reading all 137 elements in the music table.
 * On the server side. This is appropriate given the magnitude of the dataset. The backend of Lambda
* also employs queries for artist, title, and album using GSI/LSI indexes.
* searches—here, the Scan method yields the same accurate results for
 
* Calls MusicController.querySongs() (EC2/ECS backend).
 * DynamoDB table: music
 * Operation: FilterExpression Scan (AND of all supplied fields)
 */
public class QuerySongs {

    /**
     * A point of entry for independent testing only.
     * Provides an example search for 1974 Jimmy Buffett songs.
     * MusicController calls querySongs() directly in production.
     */
    public static void main(String[] args) {

        String title  = "";
        String year   = "1974";
        String artist = "Jimmy Buffett";
        String album  = "";

        List<Item> results = querySongs(title, year, artist, album);

        if (results.isEmpty()) {
            System.out.println("No result is retrieved. Please query again");
        } else {
            for (Item item : results) {
                System.out.println("----------------------------");
                System.out.println("Title:     " + item.getString("title"));
                System.out.println("Artist:    " + item.getString("artist"));
                System.out.println("Year:      " + item.getNumber("year"));
                System.out.println("Album:     " + item.getString("album"));
                System.out.println("Image URL: " + item.getString("image_url"));
            }
        }
    }

    /**
  * Uses a dynamically constructed Scan filter to search the DynamoDB music table.
     ** The runtime construction of the filter expression depends on which of the

     * There are four non-empty parameters. Every parameter supplied adds one equality.
* condition to the expression, linked with AND to the others. Parameters
* that are blank or null are just left out of the filter, thus they don't
* Limit the outcomes.
     *
* All field names utilize expression attribute names (NameMap) because
* "year" and "title" are reserved words in DynamoDB. In the absence of the # prefix alias,
* When the filter is evaluated, DynamoDB would raise a syntax error.
     *
* All values use expression attribute values (ValueMap) to avoid
* injection attacks and to manage type-safe binding (the year is bound as a
* Number; all other fields are strings.
     *

     */
    public static List<Item> querySongs(String title, String year, String artist, String album) {

        List<Item> results = new ArrayList<>();

        /*
* If none of the four fields are filled in, reject the call.
* By doing this, a scan without a filter expression is avoided, which would return* Every song in the table: according to the specification, showing every song

         * is not recommended in practical situations.
         */
        if (isEmpty(title) && isEmpty(year) && isEmpty(artist) && isEmpty(album)) {
            System.out.println("Please enter at least one search field.");
            return results;
        }

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable("music");

        /*
        * The filter expression is constructed as a string dynamically.
         * One equality condition is appended to each non-empty parameter.
         * "AND" is automatically inserted between criteria by addAnd().
         * "artist = :artist AND #year = :year" is an example of the output for artist + year.
         */
        StringBuilder filterExpression = new StringBuilder();

        /*
        * NameMap associates real DynamoDB attributes with placeholder names (such as #title).
        * names. Because "title" and "year" are DynamoDB reserved, this is necessary.* terms that are not allowed to be used directly in filter expressions—using them without
        * At runtime, the # alias results in a DynamoDB syntax error.
         */
        NameMap nameMap = new NameMap();

        /*
         * ValueMap connects the actual typed values with placeholder values (such as :artist).
         * Injection threats are avoided by using a ValueMap instead of string concatenation.* and guarantees that DynamoDB gets the appropriate attribute type for every field—
         * All other fields must be strings, and the year must be bound as a number.
         */
        ValueMap valueMap = new ValueMap();

        if (!isEmpty(title)) {
            addAnd(filterExpression);
            filterExpression.append("#title = :title");
            nameMap.with("#title", "title");
            valueMap.withString(":title", title.trim());
        }

        if (!isEmpty(year)) {
            addAnd(filterExpression);
            /*
             * The DynamoDB music table stores the year as a Number type.
             * Therefore, it needs to be connected with Number() rather than String().
             * The user-supplied string is converted to int using Integer.parseInt().
             * prior to binding. The #year alias is necessary since "year"
             * is a reserved term in DynamoDB.
             */
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

        /*
       * ScanSpec combines the value, name aliases, and filter expression string.
       * bindings into a single object that table.scan() uses. DynamoDB reads* each item in the music table, returning only those that meet the filter's requirements.
    * Items are retrieved in pages using ItemCollection, a lazy paginated iterable.
       * rather than all at once into memory while the for-each loop proceeds.
         */
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
            /*
            * Catches DynamoDB service issues (throughput exceeded, table not found,
            * NumberFormatException from parseInt, improper filter expression, etc.).
            * For CloudWatch visibility on EC2/ECS, logged to stderr.
            * Gives back an empty list, causing MusicController to show no results.
            * message instead of spreading an unmanaged server exception.
             */
            System.err.println("Unable to query songs:");
            System.err.println(e.getMessage());
        }

        return results;
    }

    /**
  * Adds "AND" to the filter expression if it already has at least
  * one condition, making sure conditions are correctly connected without a leading
  * or trailing AND.
  * An illustration of advancement
* Following the title: \title = :title"
     * Following the artist: "#title = :title AND #artist = :artist"
     * Following the album: \title = :title AND #artist = :artist AND #album = :album"
     
     * @param filterExpression The StringBuilder accumulating the expression string.
     */
    private static void addAnd(StringBuilder filterExpression) {
        if (filterExpression.length() > 0) {
            filterExpression.append(" AND ");
        }
    }

    /**
     * If the supplied string is null or consists solely of whitespace, it returns true.
     * Used to ascertain whether each query parameter was supplied.
     * in the frontend form or left empty by the user.
     * @param value The string to check.
     * @return true if null or blank, false if the string has actual content.
     */
    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}