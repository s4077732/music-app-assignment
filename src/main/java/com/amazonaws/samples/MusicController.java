package com.amazonaws.samples;

import com.amazonaws.services.dynamodbv2.document.Item;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
* The main REST controller for the Music Subscription is called MusicController.
* application, running on Amazon ECS (Fargate) and Amazon EC2.*
* Spring Boot is used in its construction, and the @RestController annotation is used to
* All return values are automatically serialized to JSON. The annotation @CrossOrigin
* permits requests from any origin (*), which is necessary since the frontend
* is hosted on a different domain for a static S3 webpage.
 *
* Every method has a direct mapping to a single HTTP endpoint. Every business reasoning and
* Dedicated helper classes handle DynamoDB operations.
* (QuerySongs, SubscribeSong, GetUserSubscriptions, UserLogin, UserRegister,
* RemoveSubscription) to maintain this controller's thinness and concentration on
* Only handling requests and responses.
 *
* The Python Lambda backend and this controller are functionally similar.
 *
 * Endpoints summary:
 *   POST   /login          – validate credentials, return user identity
 *   POST   /register       – create a new user account
 *   GET    /query          – search songs by one or more optional filters
 *   POST   /subscribe      – add a song to a user's subscription list
 *   GET    /subscriptions  – retrieve all subscriptions for a user
 *   DELETE /subscription   – remove a single subscription by song_id
 */
@RestController
@CrossOrigin(origins = "*")
public class MusicController {

    // -------------------------------------------------------------------------
    // POST /login
   
// Because Spring Boot's usual error handling is circumvented and the status is transmitted in the JSON body so the frontend JavaScript may read it directly,
//  HTTP 200 is returned even in the event of failure (rather than 401).
    // -------------------------------------------------------------------------
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {

   // Assign the Userlogin helper the task of validating credentials.
        // Userlogin compares the saved password and does a DynamoDB GetItem lookup by email (partition key). Upon success, the matched item is returned.

        // or null in the event that the password is incorrect or the email does not exist.
        Item user = Userlogin.login(body.get("email"), body.get("password"));
// To keep the return type generic, use HashMap instead of a specified response class; 
// Spring Boot's Jackson serializer automatically turns it to JSON.
        Map<String, Object> response = new HashMap<>();

        if (user != null) {
            // Credentials matched — return the user's identity to the frontend.
            // The frontend stores email and user_name in localStorage so they can
            // be displayed on the main page without a second round-trip.
            response.put("status",    "success");
            response.put("email",     user.getString("email"));
            response.put("user_name", user.getString("user_name"));
        } else {
         // Whether the password was incorrect or the email was not discovered, 
         // return the same ambiguous message. This is deliberate because an attacker might 
         // list legitimate emails with a specific message like // "email not found".

            // The assignment spec requires this exact wording.
            response.put("status",  "error");
            response.put("message", "email or password is invalid");
        }

        return response;
    }

    // -------------------------------------------------------------------------

    //
    // Note: userName is not required to be unique — only email must be unique
    // per the assignment specification.
    // -------------------------------------------------------------------------
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {

        // Delegate to UserRegister, which checks whether the email already exists
        // in the DynamoDB login table using GetItem, then writes the new user with
        // PutItem if the email is unique. Returns true on success, false if the
        // email is already taken or a DynamoDB error occurs.
        // Note: "userName" matches the camelCase field name sent by the frontend form.
        boolean success = UserRegister.register(
                body.get("email"),
                body.get("userName"),
                body.get("password")
        );

        Map<String, Object> response = new HashMap<>();

        if (success) {
            // New user created successfully. The frontend will redirect to the
            // login page after receiving this response so the user can sign in.
            response.put("status", "success");
        } else {
            // Email already exists in the login table — inform the user.
            // The assignment spec requires this exact error message wording.
            response.put("status",  "error");
            response.put("message", "The email already exists");
        }

        return response;
    }

    // -------------------------------------------------------------------------
    // GET /query?title=&year=&artist=&album=
    //
    // All four query parameters are optional, but at least one must be provided.
    // Multiple parameters are combined with AND logic — only songs matching all
    // provided fields are returned.
    //
    // Success response: JSON array of song objects, each containing:
    //   title, artist, year (Number), album, image_url
    //
    // No-results response:
    //   { "message": "No result is retrieved. Please query again" }
    //
    // Note: return type is Object (not List) because the method returns either
    // a List on success or a Map on the no-results case — two different shapes.
    // -------------------------------------------------------------------------
    @GetMapping("/query")
    public Object querySongs(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String artist,
            @RequestParam(required = false) String album
    ) {

        // Delegate to QuerySongs, which builds a DynamoDB ScanSpec filter
        // expression from whichever combination of parameters is non-null,
        // executes the scan, and returns the matching Item list.
        // An empty list is returned if no songs match or all params are null.
        List<Item> items = QuerySongs.querySongs(title, year, artist, album);
        List<Map<String, Object>> results = new ArrayList<>();

        // Convert each DynamoDB Item into a plain Map<String, Object> for
        // Jackson to serialise as a JSON object. DynamoDB Item objects cannot
        // be serialised directly by Jackson so this mapping step is required.
        for (Item item : items) {
            Map<String, Object> song = new HashMap<>();
            song.put("title",     item.getString("title"));
            song.put("artist",    item.getString("artist"));
            // item.get("year") is used instead of item.getString("year") because
            // year is stored as a Number in DynamoDB. Using getString() would
            // return null for numeric attributes. Keeping it as Object preserves
            // the numeric type in the JSON response (e.g. 2008, not "2008").
            song.put("year",      item.get("year"));
            song.put("album",     item.getString("album"));
            song.put("image_url", item.getString("image_url"));
            results.add(song);
        }

        // Return a distinct JSON object shape (not an empty array) when no results
        // are found. This lets the frontend detect the no-results case by checking
        // for the presence of a "message" key, and display the spec-required text.
        if (results.isEmpty()) {
            Map<String, String> message = new HashMap<>();
            message.put("message", "No result is retrieved. Please query again");
            return message;
        }

        return results;
    }

    // -------------------------------------------------------------------------
    // POST /subscribe
    
    // Note: duplicate subscription prevention is handled inside SubscribeSong
    // using a GetItem check before writing, so this endpoint always returns
    // success — the frontend does not need to handle a duplicate error case here.
    // -------------------------------------------------------------------------
    @PostMapping("/subscribe")
    public Map<String, Object> subscribe(@RequestBody Map<String, String> body) {

// Delegate to SubscribeSong, which uses 
// GetItem to do a duplication check before using PutItem to write the new subscription to DynamoDB.
        // Email, artist, title, year, album, and imageUrl are the order of parameters.

        // Note: imageUrl is the camelCase field name that the frontend sends;
        //  SubscribeSong stores it in DynamoDB as image_url (snake_case).
        SubscribeSong.subscribeSong(
                body.get("email"),
                body.get("artist"),
                body.get("title"),
                body.get("year"),
                body.get("album"),
                body.get("imageUrl")   // Frontend sends camelCase; SubscribeSong stores as image_url
        );

        Map<String, Object> response = new HashMap<>();
        response.put("status",  "success");
        response.put("message", "Subscribed successfully");
        return response;
    }

    // -------------------------------------------------------------------------
    // GET /subscriptions?email=user@example.com
    //
// Upon login, the "My Playlist" section of the main page is filled with all subscriptions for the specified user.
    //

    // The response is a JSON array of subscription objects, each of which has:
// (or an empty array [] if the user has no subscriptions) title, artist, year, album, image_url, song_id
    // he frontend can give the song_id directly to DELETE /subscription without rebuilding it because it is included in the response.
    
    @GetMapping("/subscriptions")
    public List<Map<String, Object>> getSubscriptions(@RequestParam String email) {

        // Delegate to GetUserSubscriptions, which performs a DynamoDB Query
        // (not Scan) using email as the partition key. This retrieves all
        // subscription items for this user in a single efficient operation.
        List<Item> items = GetUserSubscriptions.getSubscriptions(email);
        List<Map<String, Object>> results = new ArrayList<>();

        // Convert each subscription Item into a Map for JSON serialisation,
        // including song_id which the frontend needs to target DELETE requests.
        for (Item item : items) {
            Map<String, Object> sub = new HashMap<>();
            sub.put("title",     item.getString("title"));
            sub.put("artist",    item.getString("artist"));
            // year is stored as String in the subscription table (set by SubscribeSong)
            // so getString() is correct here, unlike the music table where year is a Number.
            sub.put("year",      item.getString("year"));
            sub.put("album",     item.getString("album"));
            sub.put("image_url", item.getString("image_url"));
            // song_id (format: "title_year") is the sort key of the subscription table.
            // The frontend passes this value directly to DELETE /subscription so the
            // correct item can be removed without reconstructing the key client-side.
            sub.put("song_id",   item.getString("song_id"));
            results.add(sub);
        }

        return results;
    }

    // -------------------------------------------------------------------------
    // DELETE /subscription?email=user@example.com&songId=Love+Story_2008

    // the My Playlist section's "Remove" button.
    //This is the subscription table's sort key, created by SubscribeSong at the time the item was created.
    // -------------------------------------------------------------------------
    @DeleteMapping("/subscription")
    public Map<String, Object> removeSubscription(
            @RequestParam String email,    // Partition key — identifies the user
            @RequestParam String songId    // Sort key — identifies the specific song (title_year)
    ) {
        // Delegates to RemoveSubscription, which uses the entire composite key (email + songId) to execute DynamoDB DeleteItem. DeleteItem has infinite power.

        // No error is raised if the item does not exist (e.g., has previously been removed).
        RemoveSubscription.removeSubscription(email, songId);

        Map<String, Object> response = new HashMap<>();
        response.put("status",  "success");
        response.put("message", "Subscription removed successfully");
        return response;
    }
}