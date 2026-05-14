package com.amazonaws.samples;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;

/**
 * The Music Subscription application's credential validation is handled by Userlogin.
* Verifying that a password and email provided by the user match a record in the
* "login" table in DynamoDB.

 *
 * One GetItem lookup by email (the partition key) is used in the login process.
* then contrasts the password string that was returned with the one that the user input.
 * The complete DynamoDB item is returned upon success, allowing the caller to retrieve the
* Without a second query, the user's display name and email for the frontend session.
 * In order to prevent email enumeration attacks, it returns null upon failure, regardless of whether the email was * not found or the password was incorrect.
 * Called by:  MusicController.login() (EC2/ECS backend)
 *             MusicAppLambda POST /login handler (Lambda backend)
 * DynamoDB table: login
 * Operation: GetItem (single item lookup by partition key)
 */
public class Userlogin {

    /**
* Only for stand-alone testing.
     * Prints the outcome after simulating a login with hardcoded credentials.
     * The REST controller calls login() directly in production.

     *
* Change the password or email address to a value to test the failure path.
* that doesn't correspond to any entry in the login table.
     */
    public static void main(String[] args) {

        // Test credentials — must match a record in the DynamoDB login table.
        // These correspond to one of the 10 pre-loaded users from the dataset.
        String email    = "s40777320@student.rmit.edu.au";
        String password = "012345";

        Item user = login(email, password);

        if (user != null) {
            // Login succeeded — print the user's display name to verify the
            // correct record was retrieved from DynamoDB.
            System.out.println("Login successful");
            System.out.println("User name: " + user.getString("user_name"));
        } else {
            // Login failed — matches the exact wording required by the spec.
            System.out.println("email or password is invalid");
        }
    }

    /**
     * Validates user credentials against the DynamoDB login table.
     *
    * Uses the email as the partition key to perform a GetItem lookup (O(1)).
* then contrasts the supplied password with the password string that has been stored.
     * Upon success, returns the entire item so the caller can view user_name and

     * send emails straight from it, eliminating the need for a second database round-trip.
     *
* "wrong password" and "email not found" both purposefully return null.
     * If distinct values were returned for every scenario, an attacker may
* identify the registered email addresses (enumeration attack).
     * Any null return is mapped by MusicController to the same ambiguous error message:
* "email or password is invalid"
 
     */
    public static Item login(String email, String password) {

        // Build the DynamoDB client targeting us-east-1, the region where the
        // login table was created. Credentials are sourced automatically from
        // the EC2/ECS instance's IAM role (LabRole), so no access keys are
        // hardcoded in the application code.
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        // Wrap the low-level client in the high-level DynamoDB document API.
        // This allows working with Item objects rather than raw AttributeValue
        // maps, making attribute access (e.g. user.getString("email")) simple.
        DynamoDB dynamoDB = new DynamoDB(client);

        // Reference to the login table where all user credentials are stored.
        // The table uses email as its partition key with no sort key, so each
        // email maps to exactly one item — one account per email address.
        Table table = dynamoDB.getTable("login");

        try {
            // --- Step 1: Look up the user by email ---
            // GetItem retrieves a single item directly by its partition key.
            // This is an O(1) operation that consumes one read capacity unit —
            // no scan or filter expression is needed because email is the key.
            // Returns the full Item if found, or null if the email does not exist.
            Item user = table.getItem("email", email);

            // If no item was found, the email is not registered.
            // Return null immediately — do not reveal that the email is unknown.
            if (user == null) {
                return null;
            }

            // --- Step 2: Validate the password ---
       // Get the password for this email that is kept in DynamoDB.
            //As allowed by the assignment spec, passwords are kept in plain text. They would be kept as salted hashes in an actual system and

            // compared with a constant-time function (like BCrypt.checkpw()).
            String storedPassword = user.getString("password");

            // Compare the stored password against the one the user entered.
            // String.equals() performs an exact case-sensitive comparison.
            // If they match, return the full Item so the caller can extract
            // user_name and email for the frontend session.
            if (storedPassword.equals(password)) {
                return user;  // Credentials valid — caller receives the user Item
            }

            // Passwords did not match — fall through to the null return below.
            // No early return or separate message is used here so that the
            // wrong-password and email-not-found cases are handled identically.

        } catch (Exception e) {
// Catches DynamoDB service failures such network timeouts, throughput exceeded, and table not found. sent to stderr so that, while operating on EC2/ECS for debugging visibility, problems show up in CloudWatch Logs.

            // Returns null so that instead of an unhandled server exception, MusicController provides the same error response as a failed login.
            System.err.println("Unable to login:");
            System.err.println(e.getMessage());
        }

      // Reached if either an exception was detected or the password did not match.
       // Regardless of the cause, returning null from both paths guarantees that the caller always receives the same response for every unsuccessful login.
        return null;
    }
}