package com.musicapp.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Subscription Model - Represents a User's Subscription to a Song
 *
 * Stored in DynamoDB Subscriptions table:
 * - email (Partition Key)
 * - songId (Sort Key)
 * - title, artist, album, year, img_url
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {
    private String email;       // User's email
    private String songId;      // Song ID (title#album#year)
    private String title;
    private String artist;
    private String year;
    private String album;
    private String img_url;
}