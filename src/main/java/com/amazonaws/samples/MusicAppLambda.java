package com.amazonaws.samples;

// AWS Lambda imports
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

// DynamoDB Item
import com.amazonaws.services.dynamodbv2.document.Item;

// JSON parser
import com.fasterxml.jackson.databind.ObjectMapper;

// Java utilities
import java.util.*;

/**
 * AWS Lambda Handler for API Gateway.
 * This class routes incoming HTTP requests to backend logic.
 */
public class MusicAppLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Main Lambda entry point
     */
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {

        try {
            // Get HTTP method (GET, POST, DELETE)
            String method = (String) event.get("httpMethod");

            // Get API path (/login, /query, etc.)
            String path = (String) event.get("path");

            // Get query parameters
            Map<String, String> queryParams =
                    (Map<String, String>) event.get("queryStringParameters");

            if (queryParams == null) {
                queryParams = new HashMap<>();
            }

            // Get request body (for POST requests)
            String bodyString = (String) event.get("body");
            Map<String, String> body = new HashMap<>();

            if (bodyString != null && !bodyString.isEmpty()) {
                body = mapper.readValue(bodyString, Map.class);
            }

            // ---------------- LOGIN ----------------
            if ("POST".equals(method) && "/login".equals(path)) {

                Item user = Userlogin.login(body.get("email"), body.get("password"));

                if (user == null) {
                    return response(401, map("status", "error", "message", "Invalid login"));
                }

                return response(200, map(
                        "status", "success",
                        "user_name", user.getString("user_name"),
                        "email", user.getString("email")
                ));
            }

            // ---------------- REGISTER ----------------
            if ("POST".equals(method) && "/register".equals(path)) {

                boolean success = UserRegister.register(
                        body.get("email"),
                        body.get("userName"),
                        body.get("password")
                );

                if (!success) {
                    return response(400, map("status", "error", "message", "Email already exists"));
                }

                return response(200, map("status", "success"));
            }

            // ---------------- QUERY SONGS ----------------
            if ("GET".equals(method) && "/query".equals(path)) {

                List<Item> items = QuerySongs.querySongs(
                        queryParams.get("title"),
                        queryParams.get("year"),
                        queryParams.get("artist"),
                        queryParams.get("album")
                );

                List<Map<String, Object>> results = new ArrayList<>();

                for (Item item : items) {
                    Map<String, Object> song = new HashMap<>();
                    song.put("title", item.getString("title"));
                    song.put("artist", item.getString("artist"));
                    song.put("year", item.get("year"));
                    song.put("album", item.getString("album"));
                    song.put("image_url", item.getString("image_url"));
                    results.add(song);
                }

                if (results.isEmpty()) {
                    return response(200, map("message", "No result is retrieved. Please query again"));
                }

                return response(200, results);
            }

            // ---------------- SUBSCRIBE ----------------
            if ("POST".equals(method) && "/subscribe".equals(path)) {

                SubscribeSong.subscribeSong(
                        body.get("email"),
                        body.get("artist"),
                        body.get("title"),
                        body.get("year"),
                        body.get("album"),
                        body.get("imageUrl")
                );

                return response(200, map("status", "success", "message", "Subscribed successfully"));
            }

            // ---------------- GET SUBSCRIPTIONS ----------------
            if ("GET".equals(method) && "/subscriptions".equals(path)) {

                List<Item> items = GetUserSubscriptions.getSubscriptions(queryParams.get("email"));

                List<Map<String, Object>> results = new ArrayList<>();

                for (Item item : items) {
                    Map<String, Object> sub = new HashMap<>();
                    sub.put("title", item.getString("title"));
                    sub.put("artist", item.getString("artist"));
                    sub.put("year", item.getString("year"));
                    sub.put("album", item.getString("album"));
                    sub.put("image_url", item.getString("image_url"));
                    sub.put("song_id", item.getString("song_id"));
                    results.add(sub);
                }

                return response(200, results);
            }

            // ---------------- REMOVE SUBSCRIPTION ----------------
            if ("DELETE".equals(method) && "/subscription".equals(path)) {

                RemoveSubscription.removeSubscription(
                        queryParams.get("email"),
                        queryParams.get("songId")
                );

                return response(200, map("status", "success", "message", "Removed successfully"));
            }

            // ---------------- DEFAULT ----------------
            return response(404, map("message", "Invalid API route"));

        } catch (Exception e) {
            return response(500, map("message", e.getMessage()));
        }
    }

    /**
     * Standard API response format
     */
    private Map<String, Object> response(int statusCode, Object body) {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", statusCode);

        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Headers", "Content-Type");
        headers.put("Access-Control-Allow-Methods", "GET,POST,DELETE,OPTIONS");

        response.put("headers", headers);

        try {
            response.put("body", mapper.writeValueAsString(body));
        } catch (Exception e) {
            response.put("body", "{\"message\":\"Error formatting response\"}");
        }

        return response;
    }

    /**
     * Helper method to create JSON maps easily
     */
    private Map<String, Object> map(String k1, Object v1) {
        Map<String, Object> map = new HashMap<>();
        map.put(k1, v1);
        return map;
    }

    private Map<String, Object> map(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

    private Map<String, Object> map(String k1, Object v1, String k2, Object v2, String k3, Object v3) {
        Map<String, Object> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }
}