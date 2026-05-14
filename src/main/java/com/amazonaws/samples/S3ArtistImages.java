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

/**
* S3ArtistImages is a tool for initializing data once.
 ** It extracts each picture URL from the 2026a2_songs.json dataset.

 * unique artist, uploads the image after downloading it straight from that URL
* to the specified S3 bucket in the form of an artist-named.jpg file.
 *
* In order to populate the image, this runs once prior to the application being deployed.
* bucket. The resultant S3 object key is stored by the MusicLoadData class as
* each DynamoDB music item's image_url, and the backend creates pre-signed
* URLs from those keys during query time to enable safe picture display.
 *
* Music-application-img-upload S3 bucket
 * Source data: 2026a2_songs.json (must run in the project root)
 * Us-east-1 is the AWS region.
 */
public class S3ArtistImages {

    /**
    * Entry point: reads the dataset of songs, loops through each one, and
* uploads one picture to S3 for each distinct artist.*
* Even if an artist appears in multiple songs, they are only uploaded once.
* in the dataset, since the same image appears in every song by the same artist.
     * To avoid duplicates, a HashSet keeps track of which artists have already been uploaded.
     *
     * @throws Exception if the JSON file cannot be read or parsed.
     */
    public static void main(String[] args) throws Exception {

        // S3 bucket were all artist images will be stored.
        // This bucket is kept private and images are served via pre-signed URLs
        // generat by the backend rather than being publicly accessible.
        String bucketName = "music-application-img-upload";

        // Open the JSON file containing the music dataset from the project root directory.
        // The primary entry point for reading JSON in the Jackson library is ObjectMapper.
        // readTree() creates a tree of JsonNode objects by loading the full file thus

        // Without creating a Java class, individual fields can be accessed by name.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode  = mapper.readTree(new java.io.File("2026a2_songs.json"));
        JsonNode songsNode = rootNode.get("songs");  // The top-level "songs" array

       // Create the S3 client for us-east-1, the location of the bucket's creation.
        // Credentials are automatically obtained from the environment—either the local AWS CLI configuration or the IAM role of the EC2 instance—

        // Therefore, the source code does not contain any hardcoded access keys.
        AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        // Iterator over every song object in the JSON array.
        Iterator<JsonNode> iter = songsNode.iterator();

    // Tracks that have previously been uploaded during this run with artist names.
        // Using a HashSet results in an O(1) lookup for each iteration's duplication check.
        // Instead of storing the raw name, the safe (formatted) artist name is

        // The check confirms that the key format used to name the S3 object is the same.
        Set<String> uploadedArtists = new HashSet<>();

        // Process each song in the dataset one by one.
        while (iter.hasNext()) {

            ObjectNode node = (ObjectNode) iter.next();

            // Extract the artist name and original image URL from the JSON record.
            // node.path() is used instead of node.get() because path() returns a
            // MissingNode (not null) when the field is absent, avoiding NullPointerException.
            String artist   = node.path("artist").asText();
            String imageUrl = node.path("img_url").asText();  // Field name in the dataset is img_url

            // Convert the artist name to a safe S3 object key.
            // S3 object keys cannot reliably contain spaces, apostrophes, or special
            // characters, so formatArtistName() replaces them with underscores.
            // Example: "Guns N' Roses" → "Guns_N_Roses"
            String safeArtistName = formatArtistName(artist);

            // Skip this song if the artist's image has already been uploaded.
            // Multiple songs in the dataset share the same artist, so without this
            // check the same image would be downloaded and uploaded repeatedly.
            if (uploadedArtists.contains(safeArtistName)) {
                continue;
            }

            try {
                // S3 object key format: "<SafeArtistName>.jpg"
                // Example: "Taylor_Swift.jpg"
                // This filename is what gets stored as image_url in the DynamoDB
                // music table by MusicLoadData, and it is what the backend uses
                // to construct pre-signed URLs for image retrieval.
                String fileName = safeArtistName + ".jpg";

                // Stream the image from the source URL directly into S3 without
                // saving it to disk first — see uploadImageDirectlyToS3() below.
                uploadImageDirectlyToS3(s3, bucketName, fileName, imageUrl);

                // Mark this artist as uploaded so subsequent songs by the same
                // artist are skipped in the loop above.
                uploadedArtists.add(safeArtistName);

                System.out.println("Uploaded: " + artist + " as " + fileName);

            } catch (Exception e) {
                // Log the failure and continue processing remaining artists.
                // A failed image upload should not abort the entire run —
                // the remaining artists can still be uploaded successfully.
                System.out.println("Failed: " + artist);
                System.out.println(e.getMessage());
            }
        }

        System.out.println("DONE");
    }

    /**
     * Downloads an image from a remote URL and uploads it directly to S3
     * as a streaming upload without writing any temporary file to disk.
     *
     * The image is streamed from the source URL's InputStream directly into
     * the S3 PutObject call. This avoids local disk I/O and is more memory
     * efficient than loading the entire image into a byte array first.
     *
     * Content-Length is set from the HTTP response header when available so
     * S3 can pre-allocate the object size. If the server does not return a
     * Content-Length header, S3 still accepts the upload via chunked transfer.
     *
     * @param s3        The AmazonS3 client to use for the upload.
     * @param bucketName The name of the target S3 bucket.
     * @param fileName  The S3 object key (filename) to store the image under.
     * @param imageUrl  The public URL of the source image to download.
     * @throws Exception if the URL cannot be opened or the S3 upload fails.
     */
    private static void uploadImageDirectlyToS3(
            AmazonS3 s3,
            String bucketName,
            String fileName,
            String imageUrl
    ) throws Exception {

        // Open an HTTP connection to the image source URL.
        // URLConnection handles redirects and sets appropriate request headers
        // automatically, making it suitable for downloading images from any
        // standard HTTP server.
        URL url = new URL(imageUrl);
        URLConnection connection = url.openConnection();

        // Set the S3 object metadata.
        // ContentType tells S3 (and browsers) that this object is a JPEG image,
        // ensuring it is served with the correct MIME type when accessed.
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("image/jpeg");

        // If Content-Length is provided by the HTTP response, set it.
        //The AWS SDK advises providing the content length since, in the absence of it, the SDK must buffer the entire stream in memory to

        // calculate the size before to uploading, which uses more memory.
        if (connection.getContentLengthLong() > 0) {
            metadata.setContentLength(connection.getContentLengthLong());
        }

        // Open the input stream of images and upload them to S3.
        // After the upload is finished or if an exception arises, the try-with-resources block makes sure the InputStream is always closed, preventing

        // resource leakage (hanging open network connections).
        //The actual upload is carried out by s3.putObject(), 
        // which receives data from the stream and sends it to the designated bucket and key.
        try (InputStream inputStream = connection.getInputStream()) {
            s3.putObject(bucketName, fileName, inputStream, metadata);
        }
    }
 /**
* Transforms an artist name into a secure S3 object key by substituting all
* Underscored non-alphanumeric characters that collapse sequentially

     * removing leading and trailing underscores and combining underscores into one.
     
* The format_artist_name() function in lambda.py also calls this method.
* (similar to Python) to recreate the same S3 key during query execution, so
* Both implementations must adhere to the same naming semantics.
     *
* @param artist The JSON dataset's raw artist name.
     * @return A cleaned string that can be used as a filename for an S3 object.
    */

    public static String formatArtistName(String artist) {
        return artist
                // Step 1: Replace every character that is not a letter or digit
                // with an underscore. This handles spaces, apostrophes, ampersands,
                // dots, and any other special characters.
                .replaceAll("[^a-zA-Z0-9]", "_")

                // Step 2: Collapse sequences of multiple underscores into a single one.
                // Needed because step 1 may produce "Guns_N__Roses" for "Guns N' Roses"
                // (the space and apostrophe each become an underscore).
                .replaceAll("_+", "_")

                // Step 3: Remove any leading or trailing underscores.
                // Handles artist names that start or end with a special character,
                // for example "fun." would otherwise become "fun_" after step 1.
                .replaceAll("^_|_$", "");
    }
}