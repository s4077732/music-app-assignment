package com.musicapp.backend.service;

import com.musicapp.backend.model.Subscription;
import com.musicapp.backend.util.DynamoDbUtil;
import org.springframework.stereotype.Service;
import com.musicapp.backend.model.Song;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

/**
 * SubscriptionService - Handle User Subscription operations
 *
 * Responsibilities:
 * - Get User's Subscriptions
 * - Add Song to Subscriptions
 * - Remove Song from Subscriptions
 *
 * Note: Subscriptions are stored in a separate DynamoDB table:
 * - Partition Key: email (User's email)
 * - Sort Key: songId (title#album#year)
 *
 * Source: Custom implementation based on assignment requirements
 */
@Service
public class SubscriptionService {

    private final DynamoDbUtil dynamoDbUtil;
    private final SongService songService;

    /**
     * Constructor injection
     */
    public SubscriptionService(DynamoDbUtil dynamoDbUtil, SongService songService) {
        this.dynamoDbUtil = dynamoDbUtil;
        this.songService = songService;
    }

    /**
     * Get all Subscriptions for a User
     *
     * @param email User's email
     * @return List of subscribed Songs
     */
    public List<Subscription> getUserSubscriptions(String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                System.err.println("Email is required");
                return new ArrayList<>();
            }

            List<Subscription> subscriptions = dynamoDbUtil.getSubscriptionsByEmail(email);
            System.out.println("Retrieved " + subscriptions.size() + " Subscriptions for " + email);

            return subscriptions;

        } catch (Exception e) {
            System.err.println("Error getting Subscriptions: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    /**
     * Subscribe User to a Song
     *
     * Steps:
     * 1. Validate email and songId
     * 2. Check if already subscribed (prevent duplicates)
     * 3. Get Song details from music table
     * 4. Save Subscription
     *
     * @param email User's email
     * @param selectedSong Song object selected by the user
     * @return true if Subscription successful, false otherwise
     */
    public boolean subscribeSong(String email, Song selectedSong) {
        try {
            if (email == null || email.trim().isEmpty()) {
                System.err.println("Email is required");
                return false;
            }

            if (selectedSong == null || selectedSong.getSongId() == null || selectedSong.getSongId().trim().isEmpty()) {
                System.err.println("Song information is required");
                return false;
            }

            String songId = selectedSong.getSongId();

            // Check if already subscribed
            Optional<Subscription> existing = dynamoDbUtil.getSubscription(email, songId);

            if (existing.isPresent()) {
                System.out.println("User already subscribed to this song: " + songId);
                return false;
            }

            // Copy complete song details into subscription
            Subscription subscription = new Subscription();
            subscription.setEmail(email);
            subscription.setSongId(selectedSong.getSongId());
            subscription.setTitle(selectedSong.getTitle());
            subscription.setArtist(selectedSong.getArtist());
            subscription.setYear(selectedSong.getYear());
            subscription.setAlbum(selectedSong.getAlbum());
            subscription.setImg_url(selectedSong.getImg_url());

            boolean saved = dynamoDbUtil.saveSubscription(subscription);

            if (!saved) {
                System.err.println("Subscription save failed: " + email + " -> " + songId);
                return false;
            }

            System.out.println("Subscription saved: " + email + " -> " + songId);
            return true;

        } catch (Exception e) {
            System.err.println("Error subscribing to song: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    /**
     * Remove Subscription
     *
     * @param email User's email
     * @param songId Song ID
     * @return true if removal successful, false if Subscription not found
     */
    public boolean unsubscribeSong(String email, String songId) {
        try {
            // Validate inputs
            if (email == null || email.trim().isEmpty()) {
                System.err.println("Email is required");
                return false;
            }

            if (songId == null || songId.trim().isEmpty()) {
                System.err.println("Song ID is required");
                return false;
            }

            // Check if Subscription exists
            Optional<Subscription> existing = dynamoDbUtil.getSubscription(email, songId);

            if (!existing.isPresent()) {
                System.out.println("Subscription not found: " + email + " -> " + songId);
                return false;  // Subscription doesn't exist
            }

            // Delete Subscription
            dynamoDbUtil.deleteSubscription(email, songId);
            System.out.println("Subscription deleted: " + email + " -> " + songId);

            return true;

        } catch (Exception e) {
            System.err.println("Error removing Subscription: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if User is subscribed to a Song
     *
     * @param email User's email
     * @param songId Song ID
     * @return true if subscribed, false otherwise
     */
    public boolean isSubscribed(String email, String songId) {
        try {
            Optional<Subscription> subscription = dynamoDbUtil.getSubscription(email, songId);
            return subscription.isPresent();
        } catch (Exception e) {
            System.err.println("Error checking Subscription: " + e.getMessage());
            return false;
        }
    }
}