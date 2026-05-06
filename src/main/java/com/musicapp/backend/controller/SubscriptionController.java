package com.musicapp.backend.controller;

import com.musicapp.backend.model.ApiResponse;
import com.musicapp.backend.model.Song;
import com.musicapp.backend.model.Subscription;
import com.musicapp.backend.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@CrossOrigin(origins = "*")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /**
     * GET /api/subscriptions?email=user@student.rmit.edu.au
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Subscription>>> getSubscriptions(
            @RequestParam String email) {

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.<List<Subscription>>builder()
                            .success(false)
                            .message("Email is required")
                            .data(null)
                            .build()
            );
        }

        List<Subscription> subscriptions = subscriptionService.getUserSubscriptions(email);

        return ResponseEntity.ok(
                ApiResponse.<List<Subscription>>builder()
                        .success(true)
                        .message("Subscriptions retrieved successfully")
                        .data(subscriptions)
                        .build()
        );
    }

    /**
     * POST /api/subscriptions?email=user@student.rmit.edu.au
     *
     * Request body should be a full Song object:
     * {
     *   "songId": "Love Story#Fearless#2008",
     *   "title": "Love Story",
     *   "artist": "Taylor Swift",
     *   "year": "2008",
     *   "album": "Fearless",
     *   "img_url": "https://..."
     * }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Song>> subscribe(
            @RequestParam String email,
            @RequestBody Song song) {

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.<Song>builder()
                            .success(false)
                            .message("Email is required")
                            .data(null)
                            .build()
            );
        }

        if (song == null || song.getSongId() == null || song.getSongId().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.<Song>builder()
                            .success(false)
                            .message("Song information is required")
                            .data(null)
                            .build()
            );
        }

        boolean subscribed = subscriptionService.subscribeSong(email, song);

        if (subscribed) {
            return ResponseEntity.ok(
                    ApiResponse.<Song>builder()
                            .success(true)
                            .message("Song subscribed successfully")
                            .data(song)
                            .build()
            );
        }

        return ResponseEntity.badRequest().body(
                ApiResponse.<Song>builder()
                        .success(false)
                        .message("Song is already subscribed or subscription failed")
                        .data(song)
                        .build()
        );
    }

    /**
     * DELETE /api/subscriptions?email=user@student.rmit.edu.au&songId=Love%20Story%23Fearless%232008
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> unsubscribe(
            @RequestParam String email,
            @RequestParam String songId) {

        if (email == null || email.trim().isEmpty()
                || songId == null || songId.trim().isEmpty()) {

            return ResponseEntity.badRequest().body(
                    ApiResponse.<String>builder()
                            .success(false)
                            .message("Email and songId are required")
                            .data(null)
                            .build()
            );
        }

        boolean removed = subscriptionService.unsubscribeSong(email, songId);

        if (removed) {
            return ResponseEntity.ok(
                    ApiResponse.<String>builder()
                            .success(true)
                            .message("Subscription removed successfully")
                            .data(songId)
                            .build()
            );
        }

        return ResponseEntity.badRequest().body(
                ApiResponse.<String>builder()
                        .success(false)
                        .message("Subscription not found or removal failed")
                        .data(songId)
                        .build()
        );
    }
}