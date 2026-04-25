// Adapted from project draft and AWS SDK examples for S3 upload
// Modified for EC2 + LabRole execution (no hardcoded credentials)
package com.amazonaws.samples;
import java.io.*;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.*;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class S3ArtistImages{

    public static void main(String[] args) throws Exception {

        String bucketName = "music-application-img-upload"; // 🔴 change if needed

        File jsonFile = new File("2026a2_songs.json");

        File tempDir = new File("downloaded_images");
        if (!tempDir.exists()) tempDir.mkdir();

        AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonFile);
        JsonNode songsNode = rootNode.get("songs");

        Iterator<JsonNode> iter = songsNode.iterator();
        Set<String> uploadedArtists = new HashSet<>();

        while (iter.hasNext()) {

            ObjectNode node = (ObjectNode) iter.next();

            String artist = node.path("artist").asText();
            String url = node.path("img_url").asText();

            if (uploadedArtists.contains(artist)) continue;

            try {
                String safeName = artist.replaceAll("[^a-zA-Z0-9]", "_");
                String fileName = safeName + ".jpg";

                File file = new File(tempDir, fileName);

                download(url, file);

                s3.putObject(new PutObjectRequest(bucketName, fileName, file));

                uploadedArtists.add(artist);

                System.out.println("Uploaded: " + artist);

            } catch (Exception e) {
                System.out.println("Failed: " + artist);
                System.out.println(e.getMessage());
            }
        }

        System.out.println("DONE");
    }

    private static void download(String urlStr, File file) throws Exception {
        try (InputStream in = new URL(urlStr).openStream();
             FileOutputStream out = new FileOutputStream(file)) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
}