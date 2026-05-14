package com.amazonaws.samples;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;

/**
* RemoveSubscription manages the removal of a single subscription item from the
* When a user clicks the Remove button, the DynamoDB "subscription" table is created.*
* It targets the entire composite primary via a DynamoDB DeleteItem operation.
* key (email + song_id), which enables accurate and effective deletion—
* No other subscriptions are impacted; only the specific item is eliminated.
 *
* MusicController.removeSubscription() (EC2/ECS backend) is called.
 * The Lambda backend's MusicAppLambda DELETE/subscription handler
 * DynamoDB table: subscription
* Operation: DeleteItem (precise key, single item)
 */
public class RemoveSubscription {

    /**
     * Entry point for standalone testing only.
     * Simulates a remove action using hardcoded test values.
     * In production, removeSubscription() is called directly by the REST controller.
     *
     * The songId value here must exactly match the song_id that was written
     * by SubscribeSong when the subscription was created — format: "title_year".
     */
    public static void main(String[] args) {

   // Test values: To confirm a particular deletion, substitute actual values.
        // SubscribeSong: title + "." + year must match the songId format.
        String email  = "s40777320@student.rmit.edu.au";
        String songId = "Love Story_2008";

        removeSubscription(email, songId);
    }

    /**
* Uses the DynamoDB subscription table to remove a subscription.
* A composite primary key made up of song_id and email.*
* When the user clicks Remove, the REST API layer calls this function.
* on a music card in the frontend's "My Playlist" section.
     *
* DeleteItem is idempotent; if the item is nonexistent (for instance, if the
* DynamoDB does not report an error when the user double-clicks Remove. The procedure
* just finishes silently, which is how a delete should do.
     *
* @param email   The email address of the user who is currently logged in—the partition key of the
* Subscription table. determines which user's playlist has to be changed.
     
     * The song_id entered by SubscribeSong must precisely match; otherwise,
* If there are no matching items, the DeleteItem method will silently do nothing.
     */
    public static void removeSubscription(String email, String songId) {

// Construct the DynamoDB client for the us-east-1 region, 
// which is where the subscription table was made.
//  Credentials are automatically obtained from the connected IAM role (LabRole) of the EC2/ECS instance, so no

        // The application code has hardcoded access keys.
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

       // Include the high-level DynamoDB document API around the low-level client.
        //This makes the code easier to comprehend by enabling the use of Table and Item abstractions instead of plain AttributeValue mappings.
        DynamoDB dynamoDB = new DynamoDB(client);

       // A reference to the user playlists saved in the subscription table.
        // A single song from a single user's playlist is represented by each entry in this table.
        Table table = dynamoDB.getTable("subscription");

        try {
          // Remove the subscription item that the complete composite key identifies.
            // // The subscription table's composite primary key:

            // Sort key: song_id; it uniquely identifies one song in the user's list; // Partition key: email; it groups all subscriptions for a single user; // Providing both keys targets exactly one item. This is a direct O(1) delete; neither a filter expression nor a scan are needed by DynamoDB.
            // The call ends error-free (idempotent) if there isn't an item with this key.
            table.deleteItem("email", email, "song_id", songId);

            System.out.println("Subscription removed successfully.");

        } catch (Exception e) {
          // Catches DynamoDB service failures such network timeouts, throughput exceeded, and table not found. When operating on EC2/ECS, the issue is available for debugging because it is logged to stderr and displays in CloudWatch Logs.

           // Since the frontend will reload the playlist and the item will no longer exist if the deletion is successful, it is okay that MusicController returns a success answer even though the exception is not re-thrown.
            System.err.println("Unable to remove subscription:");
            System.err.println(e.getMessage());
        }
    }
}