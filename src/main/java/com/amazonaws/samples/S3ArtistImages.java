// Adapted from AWS SDK S3 upload examples
// Modified for Assessment 2 to upload artist images directly from URL to S3

package com.amazonaws.samples;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.*;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class S3ArtistImages {

    public static void main(String[] args) throws Exception {

        String bucketName = "music-application-img-upload";

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(new java.io.File("2026a2_songs.json"));
        JsonNode songsNode = rootNode.get("songs");

        AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        Iterator<JsonNode> iter = songsNode.iterator();
        Set<String> uploadedArtists = new HashSet<>();

        while (iter.hasNext()) {

            ObjectNode node = (ObjectNode) iter.next();

            String artist = node.path("artist").asText();
            String imageUrl = node.path("img_url").asText();

            String safeArtistName = formatArtistName(artist);

            // Avoid duplicate uploads for the same artist
            if (uploadedArtists.contains(safeArtistName)) {
                continue;
            }

            try {
                String fileName = safeArtistName + ".jpg";

                uploadImageDirectlyToS3(s3, bucketName, fileName, imageUrl);

                uploadedArtists.add(safeArtistName);

                System.out.println("Uploaded: " + artist + " as " + fileName);

            } catch (Exception e) {
                System.out.println("Failed: " + artist);
                System.out.println(e.getMessage());
            }
        }

        System.out.println("DONE");
    }

    private static void uploadImageDirectlyToS3(
            AmazonS3 s3,
            String bucketName,
            String fileName,
            String imageUrl
    ) throws Exception {

        URL url = new URL(imageUrl);
        URLConnection connection = url.openConnection();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("image/jpeg");

        if (connection.getContentLengthLong() > 0) {
            metadata.setContentLength(connection.getContentLengthLong());
        }

        try (InputStream inputStream = connection.getInputStream()) {
            s3.putObject(bucketName, fileName, inputStream, metadata);
        }
    }

    // Converts artist names into safe S3 object names.
    // Example: "Guns N' Roses" -> "Guns_N_Roses", "fun." -> "fun"
    public static String formatArtistName(String artist) {
        return artist
                .replaceAll("[^a-zA-Z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }
}