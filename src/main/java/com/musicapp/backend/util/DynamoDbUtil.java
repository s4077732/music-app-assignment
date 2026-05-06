package com.musicapp.backend.util;

import com.musicapp.backend.model.Subscription;
import com.musicapp.backend.model.User;
import com.musicapp.backend.model.Song;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * DynamoDbUtil - Utility class for all DynamoDB operations
 *
 * Handles:
 * - Login table operations (User queries)
 * - Music table operations (Song queries)
 * - Subscriptions table operations (Subscription management)
 *
 * Uses AWS SDK v2 with low-level operations
 *
 * Source: Custom implementation based on assignment requirements
 */
@Component
public class DynamoDbUtil {

    private final DynamoDbClient dynamoDbClient;

    // Table names
    private static final String LOGIN_TABLE = "login";
    private static final String MUSIC_TABLE = "music";
    private static final String SUBSCRIPTIONS_TABLE = "subscriptions";

    /**
     * Constructor - Initialize DynamoDB client
     */
    public DynamoDbUtil(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    /**
     * ==================== LOGIN TABLE OPERATIONS ====================
     */

    /**
     * Get User by email from login table
     * Query by Partition Key (email)
     *
     * @param email User's email
     * @return Optional containing User if found, empty if not
     */
    public Optional<User> getUserByEmail(String email) {
        try {
            // Create query request
            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(LOGIN_TABLE)
                    .keyConditionExpression("email = :email")
                    .expressionAttributeValues(Collections.singletonMap(
                            ":email",
                            AttributeValue.builder().s(email).build()
                    ))
                    .build();

            // Execute query
            QueryResponse response = dynamoDbClient.query(queryRequest);

            if (response.items().isEmpty()) {
                System.out.println("User not found: " + email);
                return Optional.empty();
            }

            // Convert DynamoDB item to User object
            Map<String, AttributeValue> item = response.items().get(0);
            User user = new User();
            user.setEmail(getStringValue(item, "email"));
            user.setUser_name(getStringValue(item, "user_name"));
            user.setPassword(getStringValue(item, "password"));

            System.out.println("User found: " + email);
            return Optional.of(user);

        } catch (Exception e) {
            System.err.println("Error querying User: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Save new User to login table
     *
     * @param user User object to save
     * @return true if successful, false otherwise
     */
    public boolean saveUser(User user) {
        try {
            // Create item
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("email", AttributeValue.builder().s(user.getEmail()).build());
            item.put("user_name", AttributeValue.builder().s(user.getUser_name()).build());
            item.put("password", AttributeValue.builder().s(user.getPassword()).build());

            // Create put request
            PutItemRequest putRequest = PutItemRequest.builder()
                    .tableName(LOGIN_TABLE)
                    .item(item)
                    .build();

            // Execute put
            dynamoDbClient.putItem(putRequest);
            System.out.println("User saved: " + user.getEmail());
            return true;

        } catch (Exception e) {
            System.err.println("Error saving User: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ==================== MUSIC TABLE OPERATIONS ====================
     */

    /**
     * Get all Songs from music table
     *
     * Uses Scan operation (returns all items)
     * In production, would implement pagination
     *
     * @return List of all Songs
     */
    public List<Song> getAllSongs() {
        try {
            // Create scan request
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(MUSIC_TABLE)
                    .limit(1000)  // Max items per request
                    .build();

            // Execute scan
            ScanResponse response = dynamoDbClient.scan(scanRequest);

            // Convert items to Song objects
            List<Song> Songs = new ArrayList<>();

            for (Map<String, AttributeValue> item : response.items()) {
                Song song = new Song();
                song.setSongId(getStringValue(item, "songId"));
                song.setTitle(getStringValue(item, "title"));
                song.setArtist(getStringValue(item, "artist"));
                song.setYear(getStringValue(item, "year"));
                song.setAlbum(getStringValue(item, "album"));
                song.setImg_url(getStringValue(item, "img_url"));
                Songs.add(song);
            }

            System.out.println("Retrieved " + Songs.size() + " Songs from music table");
            return Songs;

        } catch (Exception e) {
            System.err.println("Error scanning music table: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Get Song by ID (artist + songId composite key)
     * Query by Partition Key (artist) and Sort Key (songId)
     *
     * @param artist Song artist (Partition Key)
     * @param songId Song ID (Sort Key) = title#album#year
     * @return Optional containing Song if found, empty if not
     */
    public Optional<Song> getSongById(String artist, String songId) {
        try {
            Map<String, AttributeValue> values = new HashMap<>();
            values.put(":artist", AttributeValue.builder().s(artist).build());
            values.put(":songId", AttributeValue.builder().s(songId).build());

            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(MUSIC_TABLE)
                    .keyConditionExpression("artist = :artist AND songId = :songId")
                    .expressionAttributeValues(values)
                    .build();

            QueryResponse response = dynamoDbClient.query(queryRequest);

            if (response.items().isEmpty()) {
                System.out.println("Song not found: " + artist + " - " + songId);
                return Optional.empty();
            }

            Map<String, AttributeValue> item = response.items().get(0);

            Song song = new Song();
            song.setSongId(getStringValue(item, "songId"));
            song.setTitle(getStringValue(item, "title"));
            song.setArtist(getStringValue(item, "artist"));
            song.setYear(getStringValue(item, "year"));
            song.setAlbum(getStringValue(item, "album"));
            song.setImg_url(getStringValue(item, "img_url"));

            System.out.println("Song found: " + artist + " - " + songId);
            return Optional.of(song);

        } catch (Exception e) {
            System.err.println("Error querying Song: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * ==================== SUBSCRIPTIONS TABLE OPERATIONS ====================
     */

    /**
     * Get all Subscriptions for a User
     * Query by Partition Key (email)
     *
     * @param email User's email
     * @return List of Subscriptions
     */
    public List<Subscription> getSubscriptionsByEmail(String email) {
        try {
            // Create query request
            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(SUBSCRIPTIONS_TABLE)
                    .keyConditionExpression("email = :email")
                    .expressionAttributeValues(Collections.singletonMap(
                            ":email",
                            AttributeValue.builder().s(email).build()
                    ))
                    .build();

            // Execute query
            QueryResponse response = dynamoDbClient.query(queryRequest);

            // Convert items to Subscription objects
            List<Subscription> Subscriptions = new ArrayList<>();

            for (Map<String, AttributeValue> item : response.items()) {
                Subscription sub = new Subscription();
                sub.setEmail(getStringValue(item, "email"));
                sub.setSongId(getStringValue(item, "songId"));
                sub.setTitle(getStringValue(item, "title"));
                sub.setArtist(getStringValue(item, "artist"));
                sub.setYear(getStringValue(item, "year"));
                sub.setAlbum(getStringValue(item, "album"));
                sub.setImg_url(getStringValue(item, "img_url"));
                Subscriptions.add(sub);
            }

            System.out.println("Retrieved " + Subscriptions.size() + " Subscriptions for " + email);
            return Subscriptions;

        } catch (Exception e) {
            System.err.println("Error querying Subscriptions: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Get single Subscription
     * Query by Partition Key (email) and Sort Key (songId)
     *
     * @param email User's email
     * @param songId Song ID
     * @return Optional containing Subscription if found, empty if not
     */
    public Optional<Subscription> getSubscription(String email, String songId) {
        try {
            Map<String, AttributeValue> values = new HashMap<>();
            values.put(":email", AttributeValue.builder().s(email).build());
            values.put(":songId", AttributeValue.builder().s(songId).build());

            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(SUBSCRIPTIONS_TABLE)
                    .keyConditionExpression("email = :email AND songId = :songId")
                    .expressionAttributeValues(values)
                    .build();

            QueryResponse response = dynamoDbClient.query(queryRequest);

            if (response.items().isEmpty()) {
                return Optional.empty();
            }

            Map<String, AttributeValue> item = response.items().get(0);

            Subscription sub = new Subscription();
            sub.setEmail(getStringValue(item, "email"));
            sub.setSongId(getStringValue(item, "songId"));
            sub.setTitle(getStringValue(item, "title"));
            sub.setArtist(getStringValue(item, "artist"));
            sub.setYear(getStringValue(item, "year"));
            sub.setAlbum(getStringValue(item, "album"));
            sub.setImg_url(getStringValue(item, "img_url"));

            return Optional.of(sub);

        } catch (Exception e) {
            System.err.println("Error querying Subscription: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }
    /**
     * Save Subscription
     *
     * @param subscription Subscription object to save
     * @return true if successful, false otherwise
     */
    public boolean saveSubscription(Subscription subscription) {
        try {
            // Create item
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("email", AttributeValue.builder().s(subscription.getEmail()).build());
            item.put("songId", AttributeValue.builder().s(subscription.getSongId()).build());
            item.put("title", AttributeValue.builder().s(subscription.getTitle()).build());
            item.put("artist", AttributeValue.builder().s(subscription.getArtist()).build());
            item.put("year", AttributeValue.builder().s(subscription.getYear()).build());
            item.put("album", AttributeValue.builder().s(subscription.getAlbum()).build());
            item.put("img_url", AttributeValue.builder().s(subscription.getImg_url()).build());

            // Create put request
            PutItemRequest putRequest = PutItemRequest.builder()
                    .tableName(SUBSCRIPTIONS_TABLE)
                    .item(item)
                    .build();

            // Execute put
            dynamoDbClient.putItem(putRequest);
            System.out.println("Subscription saved: " + subscription.getEmail() + " -> " + subscription.getSongId());
            return true;

        } catch (Exception e) {
            System.err.println("Error saving Subscription: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete Subscription
     *
     * @param email User's email
     * @param songId Song ID
     * @return true if successful, false otherwise
     */
    public boolean deleteSubscription(String email, String songId) {
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("email", AttributeValue.builder().s(email).build());
            key.put("songId", AttributeValue.builder().s(songId).build());

            DeleteItemRequest deleteRequest = DeleteItemRequest.builder()
                    .tableName(SUBSCRIPTIONS_TABLE)
                    .key(key)
                    .build();

            dynamoDbClient.deleteItem(deleteRequest);

            System.out.println("Subscription deleted: " + email + " -> " + songId);
            return true;

        } catch (Exception e) {
            System.err.println("Error deleting Subscription: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ==================== HELPER METHODS ====================
     */

    /**
     * Helper - Extract string value from DynamoDB item
     *
     * @param item DynamoDB item
     * @param key Attribute key
     * @return String value or empty string if not found
     */
    private String getStringValue(Map<String, AttributeValue> item, String key) {
        if (item.containsKey(key)) {
            AttributeValue value = item.get(key);
            if (value.s() != null) {
                return value.s();
            }
        }
        return "";
    }

    /**
     * Helper - Extract number value from DynamoDB item
     *
     * @param item DynamoDB item
     * @param key Attribute key
     * @return Number value or 0 if not found
     */
    private int getNumberValue(Map<String, AttributeValue> item, String key) {
        if (item.containsKey(key)) {
            AttributeValue value = item.get(key);
            if (value.n() != null) {
                return Integer.parseInt(value.n());
            }
        }
        return 0;
    }
}