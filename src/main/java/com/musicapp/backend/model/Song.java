package com.musicapp.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Song Model - Represents a Song from DynamoDB music table
 * Attributes from DynamoDB:
 * - artist (Partition Key)
 * - songId (Sort Key) = title#album#year
 * - title
 * - album
 * - year
 * - img_url
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Song {
    private String songId;      // Composite key: title#album#year
    private String title;
    private String artist;
    private String year;
    private String album;
    private String img_url;
}