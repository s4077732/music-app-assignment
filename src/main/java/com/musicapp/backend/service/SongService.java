package com.musicapp.backend.service;

import com.musicapp.backend.model.Song;
import com.musicapp.backend.util.DynamoDbUtil;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * SongService - Handle Song search operations
 *
 * Responsibilities:
 * - Search Songs by title, artist, year, album
 * - Filter results with AND logic (all criteria must match)
 * - Get Song by ID
 *
 * Source: Custom implementation based on assignment requirements
 */
@Service
public class SongService {

    private final DynamoDbUtil dynamoDbUtil;

    /**
     * Constructor injection of DynamoDbUtil
     */
    public SongService(DynamoDbUtil dynamoDbUtil) {
        this.dynamoDbUtil = dynamoDbUtil;
    }

    /**
     * Search Songs with multiple criteria
     *
     * Filters are applied with AND logic:
     * - If title is provided, Songs MUST match title
     * - If artist is provided, Songs MUST match artist
     * - If year is provided, Songs MUST match year
     * - If album is provided, Songs MUST match album
     *
     * At least one filter must be provided
     *
     * Example queries:
     * - title="Love Story" → Find Songs with title "Love Story"
     * - artist="Taylor Swift" & album="Fearless" → Find Taylor Swift Songs in Fearless
     * - artist="Jimmy Buffett" & year="1974" → Find Jimmy Buffett Songs from 1974
     *
     * @param title Song title (optional)
     * @param artist Song artist (optional)
     * @param year Song year (optional)
     * @param album Song album (optional)
     * @return List of matching Songs (empty if no matches)
     */
    public List<Song> searchSongs(String title, String artist, String year, String album) {
        try {
            // At least one filter must be provided
            if (isEmpty(title) && isEmpty(artist) && isEmpty(year) && isEmpty(album)) {
                System.out.println("No search criteria provided");
                return new ArrayList<>();
            }

            // Get all Songs from music table
            List<Song> allSongs = dynamoDbUtil.getAllSongs();

            if (allSongs == null || allSongs.isEmpty()) {
                System.out.println("No Songs found in database");
                return new ArrayList<>();
            }

            // Apply filters with AND logic
            List<Song> results = allSongs.stream()
                    .filter(song -> isEmpty(title) ||
                            song.getTitle().toLowerCase().contains(title.toLowerCase()))

                    .filter(song -> isEmpty(artist) ||
                            song.getArtist().toLowerCase().contains(artist.toLowerCase()))

                    .filter(song -> isEmpty(year) || song.getYear().equals(year))

                    .filter(song -> isEmpty(album) ||
                            song.getAlbum().toLowerCase().contains(album.toLowerCase()))

                    .collect(Collectors.toList());

            System.out.println("Search completed. Found " + results.size() + " Songs");
            System.out.println("  Criteria: title=" + title + ", artist=" + artist +
                    ", year=" + year + ", album=" + album);

            return results;

        } catch (Exception e) {
            System.err.println("Error during Song search: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    /**
     * Get Song by ID (songId = title#album#year)
     *
     * @param artist Song artist
     * @param songId Song ID
     * @return Song object if found, empty if not found
     */
    public java.util.Optional<Song> getSongById(String artist, String songId) {
        try {
            return dynamoDbUtil.getSongById(artist, songId);
        } catch (Exception e) {
            System.err.println("Error getting Song: " + e.getMessage());
            e.printStackTrace();
            return java.util.Optional.empty();
        }
    }

    /**
     * Helper method - Check if string is null or empty
     */
    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}