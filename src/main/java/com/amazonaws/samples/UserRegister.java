package com.amazonaws.samples;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;

/**
* UserRegister manages the creation of new Music Subscription user accounts.* program, adding new user entries to the "login" table in DynamoDB.
 * It checks for duplicate emails using* GetItem. Registration is denied if the email address is already registered.* and "The email already exists" appears on the frontend. If the email is* distinct, the frontend reroutes and the new user is written using PutItem
Go to the page where you log in.
 * In accordance with the assignment requirements:
 * Every user's email address must be distinct (enforced here via GetItem check).
 * A username need not be unique; two people may use the same one.
 * As allowed for this assignment, passwords are kept in plain text.
* Passwords must always be salted and hashed in real-world systems.
 * DynamoDB table: login
 * Operations: GetItem (duplicate check) + PutItem (write new user)
 */
public class UserRegister {

    /**
* Only for stand-alone testing.
     * Uses hardcoded test values to simulate a registration and publishes the outcome.
     * The REST controller calls register() immediately in production..
     */
    public static void main(String[] args) {

      // Test values: either use an existing email to confirm the duplicate 
      // rejection path or substitute any email that isn't currently in the login database to confirm a successful signup.
        String email    = "newuser@student.rmit.edu.au";
        String userName = "NewUser";
        String password = "123456";

        boolean registered = register(email, userName, password);

        if (registered) {
            System.out.println("User registered successfully!");
        } else {
            // This message matches the exact wording required by the assignment spec.
            System.out.println("The email already exists");
        }
    }

    /**
     * If the email address is unique, a new user is registered in the DynamoDB login database.
     ** The technique first determines if the email address is already registered using a
     * GetItem lookup (O(1), which directly addresses the partition key). Only in the event that
     * If the email is verified to be new, does it write the user with PutItem?
     * Instead of using a DynamoDB, this two-step check-then-write method uses a conditional expression to give the caller a clear boolean signal indicating whether the registration was accepted or rejected, which
     * MusicController maps to the proper JSON response for the frontend.
     * @param email    the email address that the user provided when registering.
     * Used as the login table's partition key; it needs to be distinct.
     * @param userName the display name that the user input. kept for exhibition on

     * The main page following login. Being distinctive is not necessary.
     * @param password the password that the user typed. kept in plain text as
* Permitted under the assignment specifications. Hashing is required in production.
     * @return true if the new user was successfully written and the email was unique.
     * If the email is already present in the table, or if a DynamoDB
* The GetItem or PutItem action encounters an error.
     */
    public static boolean register(String email, String userName, String password) {

        // Build the DynamoDB client targeting us-east-1, the region where the
        // login table was created. Credentials are sourced automatically from
        // the EC2/ECS instance's IAM role (LabRole), so no access keys are
        // hardcoded in the application code.
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        // Wrap the low-level client in the high-level DynamoDB document API.
        // This allows working with Item objects rather than raw AttributeValue
        // maps, making the code significantly more readable.
        DynamoDB dynamoDB = new DynamoDB(client);

        // Reference to the login table where all user credentials are stored.
        // The login table has email as its partition key and no sort key,
        // so each email address corresponds to exactly one item.
        Table table = dynamoDB.getTable("login");

        try {
            // --- Step 1: Duplicate email check ---
           // Use email as the partition key when doing a GetItem lookup.
            //For this specific check, GetItem is significantly more efficient than a Scan because it is O(1) and only uses one read capacity unit.

            // If the email is present, it returns the entire item; otherwise, it returns null.
            Item existingUser = table.getItem("email", email);

            if (existingUser != null) {
                // Email is already registered — reject the registration.
                // MusicController maps this false return to the JSON response:
                // { "status": "error", "message": "The email already exists" }
                return false;
            }

            // --- Step 2: Write the new user ---
           // The email is verified to be unique; create the new user object.
            // There are three characteristics in the login table schema:
// The user is uniquely identified by their email and partition key.

            A//fter logging in, the user's name is displayed on the main page.
            // The password is stored as plain text and is allowed for this assignment.



            Item newUser = new Item()
                    .withPrimaryKey("email", email)
                    .withString("user_name", userName)
                    .withString("password",  password);

            
            //// The new user is written to the login table using PutItem.
           // Under typical circumstances, this won't overwrite any existing item because we already verified the email doesn't exist in Step 1.

            //(An overwrite could result from a highly improbable race scenario in which two registrations with the same email arrive at the same time, 
            //but this is acceptable for the purposes of this assignment.)


            table.putItem(newUser);

            // Registration succeeded — return true so MusicController responds
            // with { "status": "success" } and the frontend redirects to login.
            return true;

        } catch (Exception e) {
           // Catches Table not found, throughput exceeded, and network timeouts are examples of DynamoDB service issues.
           //  When operating on EC2/ECS, failures are logged to stderr, making them visible for troubleshooting in CloudWatch Logs.

            Returning false prevents a deceptive success message on failure by causing MusicController to deliver an error response to the frontend.
            System.err.println("Unable to register user:");
            System.err.println(e.getMessage());
            return false;
        }
    }
}