package com.musicapp.backend.controller;

import com.musicapp.backend.model.ApiResponse;
import com.musicapp.backend.model.Song;
import com.musicapp.backend.service.SongService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/songs")
@CrossOrigin(origins = "*")
public class SongController {

    private final SongService songService;

    public SongController(SongService songService) {
        this.songService = songService;
    }

    /**
     * GET /api/songs/search?title=...&artist=...&year=...&album=...
     *
     * All parameters are optional, but at least one should be supplied.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Song>>> searchSongs(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String artist,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String album) {

        List<Song> songs = songService.searchSongs(title, artist, year, album);

        if (songs.isEmpty()) {
            return ResponseEntity.ok(
                    ApiResponse.<List<Song>>builder()
                            .success(false)
                            .message("No matching songs found")
                            .data(songs)
                            .build()
            );
        }

        return ResponseEntity.ok(
                ApiResponse.<List<Song>>builder()
                        .success(true)
                        .message("Songs retrieved successfully")
                        .data(songs)
                        .build()
        );
    }
}