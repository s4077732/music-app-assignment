package com.musicapp.backend.util;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

/**
 * S3Util - Utility class for S3 operations
 * Handles:
 * - Generating S3 image URLs
 * - Image URL transformations
 * Source: Custom implementation based on assignment requirements
 */
@Component
public class S3Util {

    // S3 bucket name for artist images
    // Make sure this matches your bucket name
    @Value("${aws.s3.bucket.name:music-app-images-s4113122}")
    private String bucketName;

    // S3 region
    @Value("${aws.s3.region:us-east-1}")
    private String region;

    /**
     * Generate S3 image URL from original image URL
     *
     * The JSON file contains image URLs like:
     * "https://raw.githubusercontent.com/YingZhang2015/cc/main/TaylorSwift.jpg"
     *
     * We need to convert this to S3 URL:
     * "https://music-app-images-s4113122.s3.amazonaws.com/TaylorSwift.jpg"
     *
     * @param originalImageUrl Original image URL from JSON
     * @return S3 image URL
     */
    public String getS3ImageUrl(String originalImageUrl) {
        if (originalImageUrl == null || originalImageUrl.trim().isEmpty()) {
            return null;
        }

        try {
            // Extract filename from original URL
            // Example: "https://raw.githubusercontent.com/.../TaylorSwift.jpg" → "TaylorSwift.jpg"
            String filename = extractFilenameFromUrl(originalImageUrl);

            // Construct S3 URL
            return String.format("https://%s.s3.%s.amazonaws.com/%s",
                    bucketName, region, filename);

        } catch (Exception e) {
            System.err.println("Error generating S3 URL: " + e.getMessage());
            return originalImageUrl;  // Return original URL as fallback
        }
    }

    /**
     * Extract filename from URL
     * Example:
     * Input: "https://raw.githubusercontent.com/YingZhang2015/cc/main/TaylorSwift.jpg"
     * Output: "TaylorSwift.jpg"
     *
     * @param imageUrl Full image URL
     * @return Filename
     */
    private String extractFilenameFromUrl(String imageUrl) {
        int lastSlashIndex = imageUrl.lastIndexOf('/');
        if (lastSlashIndex != -1 && lastSlashIndex < imageUrl.length() - 1) {
            return imageUrl.substring(lastSlashIndex + 1);
        }
        return imageUrl;  // Return original if can't extract
    }

    /**
     * Get bucket name
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Set bucket name (for testing)
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Get region
     */
    public String getRegion() {
        return region;
    }

    /**
     * Set region (for testing)
     */
    public void setRegion(String region) {
        this.region = region;
    }
}