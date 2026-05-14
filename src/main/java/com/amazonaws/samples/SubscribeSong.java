package com.amazonaws.samples;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;

 /*** /*Adding a song to a user's subscription list is handled by SubscribeSong.
 It uses a new item to be added to the DynamoDB "subscription" table.
 a composite primary key made up of song_id (sort key) and email (partition key).
  Before writing, a duplicate check is done to avoid the same* The same user has several subscriptions to the same song.
 Called by: MusicAppLambda (Lambda) and MusicController (EC2/ECS)
 *DynamoDB table: subscription* Operation: PutItem (write) + GetItem (duplicate check)
 ***/ 
public class SubscribeSong {

    /**
   * Only for stand-alone testing.
     * Uses hardcoded test values to simulate a subscribe operation.
     * The REST controller calls subscribeSong() directly in production.
     */
    public static void main(String[] args) {
        subscribeSong(
                "s40777320@student.rmit.edu.au",   // test user email
                "Taylor Swift",                     // artist
                "Love Story",                       // title
                "2008",                             // year
                "Fearless",                         // album
                "https://music-application-img-upload.s3.amazonaws.com/Taylor_Swift.jpg"
        );
    }

    /**
     * In the DynamoDB subscription table, a user's song subscription is added.
     ** The REST API layer (MusicController or Lambda) calls this method.

     * when the user presses the front-end Subscribe button.
     *
     * @param email    The subscription table's partition key is the email address of the logged-in user.
     * @param artist The name of the artist, saved for the subscription area's presentation.
     * @param title The song title, which is used to create song_id and stored for presentation.
     * @param year     The song_id is constructed using the release year as a String.
     * @param album    The name of the album, saved for the subscription area's display.
     * @param imageUrl The artist image's S3 URL, saved so the subscription
* The area does not need to re-query the music table in order to display photos.
     */
    public static void subscribeSong(String email, String artist, String title,
                                     String year, String album, String imageUrl) {
// Target us-east-1, where all tables are deployed, when building the DynamoDB client.
        // Credentials are automatically retrieved from the IAM role of the EC2/ECS instance.

        // (LabRole), hence the program does not contain any hardcoded access keys.
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        // Wrap the low-level client in the high-level DynamoDB document API,
        // which lets us work with Item objects instead of raw AttributeValue maps.
        DynamoDB dynamoDB = new DynamoDB(client);

        // Reference to the subscription table where user playlists are stored.
        Table table = dynamoDB.getTable("subscription");

        // Create the sort key by adding an underscore to the title and the year.
        // Format: "Love Story_2008" // This composite key guarantees originality even when two distinct songs share

        Despite having the same title, they were published in different years.
        // For DELETE to function properly, it must match the song_id format given in RemoveSubscription.
        String songId = title + "_" + year;

        // To determine whether this user has already subscribed to this song, perform a GetItem lookup using the complete composite key (email + song_id).
        // GetItem utilizes only one item and is O(1).

        // far more effective than a query or scan for this specific check.
        Item existingItem = table.getItem("email", email, "song_id", songId);

        if (existingItem != null) {
            // Subscription already exists — silently return to avoid duplicates.
            // The frontend also handles this gracefully when status "exists" is returned.
            System.out.println("Already subscribed!");
            return;
        }

        // Build the new subscription item with all attributes needed by the frontend
        // to display the song card (title, artist, year, album, image).
        // email + song_id form the composite primary key of the subscription table:
        //   email    = partition key (groups all subscriptions for one user)
        //   song_id  = sort key (uniquely identifies the song within that user's list)
        Item item = new Item()
                .withPrimaryKey("email", email, "song_id", songId)
                .withString("artist",    artist)
                .withString("title",     title)
                .withString("year",      year)
                .withString("album",     album)
                .withString("image_url", imageUrl);  // Stored so images load without re-querying music table

        try {
          // PutItem updates DynamoDB with the new subscription.
// If, in whatever way, an item with the same key already existing (race condition),
// PutItem will replace it with the identical information without causing any damage.
            table.putItem(item);
            System.out.println("Subscription added successfully.");

        } catch (Exception e) {
            // Catches DynamoDB service errors (e.g. table not found, throttling,
            // network timeout). The error is logged to stderr for CloudWatch visibility.
            System.err.println("Unable to subscribe:");
            System.err.println(e.getMessage());
        }
    }
}