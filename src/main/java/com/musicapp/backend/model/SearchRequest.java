package com.musicapp.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Search Request DTO.
 *
 * Used when frontend sends search criteria.
 *
 * Example:
 * {
 *   "title": "Love Story",
 *   "artist": "Taylor Swift",
 *   "year": "2008",
 *   "album": "Fearless"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {
    private String title;
    private String artist;
    private String year;
    private String album;
}