package com.musicapp.initialization;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.sync.RequestBody;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * ASSIGNMENT REQUIREMENT 4: Write a program that automatically downloads all artist 
 * images based on the image_url values found in 2026a2_songs.json and then uploads 
 * these images to an S3 bucket.
 *
 * Key Points:
 * - Multiple songs can have the same artist image (e.g., Taylor Swift image used for 7 songs)
 * - We should only download UNIQUE images (by URL, not by artist)
 * - Total unique images: ~50+ (not 136, since many artists share the same image URL)
 *
 * Process:
 * 1. Read 2026a2_songs.json
 * 2. Extract all image_url values
 * 3. Find UNIQUE URLs (remove duplicates)
 * 4. Download each image from the URL
 * 5. Upload to S3 bucket
 * 6. Track success/failures
 */
public class UploadImages {

    // Configuration
    private static final String S3_BUCKET_NAME = "group126imagebucket";  // Change this!
    private static final String TEMP_DOWNLOAD_DIR = "downloaded_images";
    private static final int CONNECTION_TIMEOUT = 5000;  // 5 seconds
    private static final int READ_TIMEOUT = 5000;        // 5 seconds

    public static void main(String[] args) throws Exception {

        S3Client s3Client = S3Client.builder()
                .region(software.amazon.awssdk.regions.Region.US_EAST_1)
                .build();

        try {
            System.out.println("==== Downloading and Uploading Images ====\n");

            // Step 1: Create temp directory
            createTempDirectory();

            // Step 2: Read JSON and extract unique image URLs
            System.out.println("Step 1: Reading JSON file and extracting image URLs...");
            Set<String> uniqueImageUrls = extractUniqueImageUrls("2026a2_songs.json");
            System.out.println("✓ Found " + uniqueImageUrls.size() + " unique image URLs\n");

            // Step 3: Download images
            System.out.println("Step 2: Downloading images to local directory...");
            Map<String, String> downloadedImages = downloadImages(uniqueImageUrls);
            System.out.println("✓ Downloaded " + downloadedImages.size() + " images\n");

            // Step 4: Upload to S3
            System.out.println("Step 3: Uploading images to S3 bucket: " + S3_BUCKET_NAME);
            uploadImagesToS3(s3Client, downloadedImages);

            // Step 5: Verify upload
            System.out.println("\nStep 4: Verifying uploaded images...");
            verifyImagesInS3(s3Client);

            System.out.println("\n✓ ALL IMAGES PROCESSED SUCCESSFULLY!");
            System.out.println("  - S3 Bucket: " + S3_BUCKET_NAME);
            System.out.println("  - Total unique images: " + uniqueImageUrls.size());
            System.out.println("  - Downloaded: " + downloadedImages.size());

        } catch (Exception e) {
            System.err.println("✗ ERROR: " + e.getMessage());
            e.printStackTrace();
        } finally {
            s3Client.close();
            cleanupTempDirectory();
        }
    }

    /**
     * Create temporary directory for downloaded images
     */
    private static void createTempDirectory() {
        File tempDir = new File(TEMP_DOWNLOAD_DIR);
        if (!tempDir.exists()) {
            tempDir.mkdir();
            System.out.println("✓ Created temp directory: " + TEMP_DOWNLOAD_DIR);
        }
    }

    /**
     * Read JSON file and extract all UNIQUE image URLs
     *
     * Why unique? Because multiple songs can have the same artist image.
     * For example:
     * - "Love Story" (Taylor Swift) uses TaylorSwift.jpg
     * - "Bad Blood" (Taylor Swift) also uses TaylorSwift.jpg
     * - "We Are Never Ever Getting Back Together" (Taylor Swift) also uses TaylorSwift.jpg
     *
     * So we only need to download TaylorSwift.jpg once!
     */
    private static Set<String> extractUniqueImageUrls(String jsonFilePath) throws Exception {
        Set<String> uniqueUrls = new HashSet<>();

        try {
            String jsonContent = new String(java.nio.file.Files.readAllBytes(
                    java.nio.file.Paths.get(jsonFilePath)
            ));

            com.google.gson.JsonArray songsArray = com.google.gson.JsonParser
                    .parseString(jsonContent)
                    .getAsJsonObject()
                    .getAsJsonArray("songs");

            for (int i = 0; i < songsArray.size(); i++) {
                com.google.gson.JsonObject song = songsArray.get(i).getAsJsonObject();
                String imgUrl = song.get("img_url").getAsString();
                uniqueUrls.add(imgUrl);
            }

            System.out.println("✓ Extracted " + uniqueUrls.size() + " unique image URLs");
            return uniqueUrls;

        } catch (Exception e) {
            System.err.println("✗ Error reading JSON: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Download images from URLs and save to local directory
     *
     * Returns a Map of:
     * - Key: Image filename (extracted from URL)
     * - Value: Full file path
     */
    private static Map<String, String> downloadImages(Set<String> imageUrls) {
        Map<String, String> downloadedFiles = new HashMap<>();
        int successCount = 0;
        int failCount = 0;

        for (String imageUrl : imageUrls) {
            try {
                // Extract filename from URL
                // Example: "https://raw.githubusercontent.com/YingZhang2015/cc/main/TaylorSwift.jpg"
                // Filename: "TaylorSwift.jpg"
                String filename = extractFilenameFromUrl(imageUrl);
                String filePath = TEMP_DOWNLOAD_DIR + File.separator + filename;

                // Skip if already downloaded
                if (new File(filePath).exists()) {
                    System.out.println("  ⊘ Already exists: " + filename);
                    downloadedFiles.put(filename, filePath);
                    continue;
                }

                // Download image
                downloadImage(imageUrl, filePath);
                downloadedFiles.put(filename, filePath);
                successCount++;

                System.out.println("  ✓ Downloaded: " + filename);

            } catch (Exception e) {
                failCount++;
                System.err.println("  ✗ Failed to download: " + imageUrl);
                System.err.println("    Error: " + e.getMessage());
            }
        }

        System.out.println("\n✓ Download summary:");
        System.out.println("  - Success: " + successCount);
        System.out.println("  - Failed: " + failCount);
        System.out.println("  - Skipped: " + (imageUrls.size() - successCount - failCount));

        return downloadedFiles;
    }

    /**
     * Download a single image from URL and save to file
     */
    private static void downloadImage(String imageUrl, String filePath) throws Exception {
        URL url = new URL(imageUrl);
        URLConnection connection = url.openConnection();

        // Set timeouts to prevent hanging
        connection.setConnectTimeout(CONNECTION_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);

        // Set User-Agent header (some servers block requests without it)
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

        try (InputStream inputStream = connection.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(filePath)) {

            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    /**
     * Extract filename from URL
     * Example: "https://raw.githubusercontent.com/.../TaylorSwift.jpg" → "TaylorSwift.jpg"
     */
    private static String extractFilenameFromUrl(String imageUrl) {
        return imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
    }

    /**
     * Upload downloaded images to S3 bucket
     */
    private static void uploadImagesToS3(S3Client s3Client, Map<String, String> downloadedFiles) {
        int successCount = 0;
        int failCount = 0;

        for (Map.Entry<String, String> entry : downloadedFiles.entrySet()) {
            String filename = entry.getKey();
            String filePath = entry.getValue();

            try {
                File file = new File(filePath);

                // Create PutObject request
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(S3_BUCKET_NAME)
                        .key(filename)  // S3 object key (filename)
                        .build();

                // Upload file
                s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));

                successCount++;
                System.out.println("  ✓ Uploaded to S3: " + filename);

            } catch (NoSuchBucketException e) {
                failCount++;
                System.err.println("  ✗ S3 Bucket not found: " + S3_BUCKET_NAME);
                System.err.println("    Make sure you created the bucket in AWS Console!");
                throw new RuntimeException("S3 bucket does not exist: " + S3_BUCKET_NAME);

            } catch (Exception e) {
                failCount++;
                System.err.println("  ✗ Failed to upload: " + filename);
                System.err.println("    Error: " + e.getMessage());
            }
        }

        System.out.println("\n✓ Upload summary:");
        System.out.println("  - Success: " + successCount);
        System.out.println("  - Failed: " + failCount);
    }

    /**
     * Verify images were uploaded to S3
     */
    private static void verifyImagesInS3(S3Client s3Client) {
        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(S3_BUCKET_NAME)
                    .build();

            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

            System.out.println("✓ Images in S3 bucket:");
            for (S3Object obj : listResponse.contents()) {
                long sizeInKB = obj.size() / 1024;
                System.out.println("  - " + obj.key() + " (" + sizeInKB + " KB)");
            }

        } catch (NoSuchBucketException e) {
            System.err.println("✗ Bucket not found: " + S3_BUCKET_NAME);
        } catch (Exception e) {
            System.err.println("✗ Error verifying images: " + e.getMessage());
        }
    }

    /**
     * Clean up temporary directory after upload
     */
    private static void cleanupTempDirectory() {
        try {
            File tempDir = new File(TEMP_DOWNLOAD_DIR);
            if (tempDir.exists()) {
                File[] files = tempDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
                tempDir.delete();
                System.out.println("\n✓ Cleaned up temp directory");
            }
        } catch (Exception e) {
            System.out.println("⚠ Warning: Could not cleanup temp directory: " + e.getMessage());
        }
    }
}