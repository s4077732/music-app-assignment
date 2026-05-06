package com.amazonaws.samples;

import com.amazonaws.services.dynamodbv2.document.Item;
import org.springframework.web.bind.annotation.*;

import java.util.*;

// REST controller for EC2 and ECS.
// UI calls these HTTP endpoints.
@RestController
@CrossOrigin(origins = "*")
public class MusicController {

    // POST /login
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {

        Item user = Userlogin.login(body.get("email"), body.get("password"));

        Map<String, Object> response = new HashMap<>();

        if (user != null) {
            response.put("status", "success");
            response.put("email", user.getString("email"));
            response.put("user_name", user.getString("user_name"));
        } else {
            response.put("status", "error");
            response.put("message", "email or password is invalid");
        }

        return response;
    }

    // POST /register
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {

        boolean success = UserRegister.register(
                body.get("email"),
                body.get("userName"),
                body.get("password")
        );

        Map<String, Object> response = new HashMap<>();

        if (success) {
            response.put("status", "success");
        } else {
            response.put("status", "error");
            response.put("message", "The email already exists");
        }

        return response;
    }

    // GET /query?artist=Taylor Swift&album=Fearless
    @GetMapping("/query")
    public Object querySongs(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String artist,
            @RequestParam(required = false) String album
    ) {

        List<Item> items = QuerySongs.querySongs(title, year, artist, album);
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
            Map<String, String> message = new HashMap<>();
            message.put("message", "No result is retrieved. Please query again");
            return message;
        }

        return results;
    }

    // POST /subscribe
    @PostMapping("/subscribe")
    public Map<String, Object> subscribe(@RequestBody Map<String, String> body) {

        SubscribeSong.subscribeSong(
                body.get("email"),
                body.get("artist"),
                body.get("title"),
                body.get("year"),
                body.get("album"),
                body.get("imageUrl")
        );

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Subscribed successfully");
        return response;
    }

    // GET /subscriptions?email=s40777320@student.rmit.edu.au
    @GetMapping("/subscriptions")
    public List<Map<String, Object>> getSubscriptions(@RequestParam String email) {

        List<Item> items = GetUserSubscriptions.getSubscriptions(email);
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

        return results;
    }

    // DELETE /subscription?email=...&songId=Love Story_2008
    @DeleteMapping("/subscription")
    public Map<String, Object> removeSubscription(
            @RequestParam String email,
            @RequestParam String songId
    ) {

        RemoveSubscription.removeSubscription(email, songId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Subscription removed successfully");
        return response;
    }
}