
# Handles login, register, query, subscribe, get subscriptions, and remove.

import json
import boto3
from decimal import Decimal
from boto3.dynamodb.conditions import Attr, Key

# AWS Resource Initialisation # 
#botA high-level DynamoDB interface (Tables, Items) is provided by o3.resource instead
# compared to the raw JSON dicts of the low-level client.
#  The region # where each of the three DynamoDB tables was created must match us-east-1.
dynamodb = boto3.resource("dynamodb", region_name="us-east-1")

# In order to prevent repetitive traversals to the DynamoDB endpoint on each request, 
# table references are established once at the module level (outside the handler)
#  and then reused across warm Lambda invocations.
login_table        = dynamodb.Table("login")         # Stores user credentials
music_table        = dynamodb.Table("music")         # Stores the song catalogue
subscription_table = dynamodb.Table("subscription")  # Stores per-user subscriptions


# --------------------------------- — Help: decimal_default # -----------------------
#  Numbers are stored as Python Decimal objects in DynamoDB.
#  Decimal cannot be serialized by Python's built-in json.dumps() function, 
# therefore this custom serializer first converts them to int before encoding. 
# In the respond() helper below, 
# it is supplied as the `default` argument to #json.dumps().
def decimal_default(obj):
    if isinstance(obj, Decimal):
        return int(obj)
    raise TypeError  # Let json.dumps raise its normal error for other types


# ---------------------------------------------------------------------------
# Helper: response
# ---------------------------------------------------------------------------
# helps to the standard API Gateway proxy response dict that Lambda must return.
# for the brwoser not to block Every HTTP response include CORS headers 
# cross-origin requests from the S3-hosted frontend.
#   Access-Control-Allow-Origin: *   — allows any domain (S3 static site)
#   Access-Control-Allow-Methods     — lists every HTTP verb the API uses
def response(status_code, body):
    return {
        "statusCode": status_code,
        "headers": {
            "Access-Control-Allow-Headers": "Content-Type",
            "Access-Control-Allow-Origin":  "*",
            "Access-Control-Allow-Methods": "GET,POST,DELETE,OPTIONS"
        },
        "body": json.dumps(body, default=decimal_default)
    }


# ---------------------------------------------------------------------------
# Helper: get_body
# ---------------------------------------------------------------------------
# The HTTP request content is given by API Gateway as a JSON string inside event ("body").
#  It is safely parsed into a Python dict using this helper. Callers can safely use.get()
#  because an empty dict is returned if the body is absent or the JSON is corrupted.
def get_body(event):
    if event.get("body"):
        try:
            return json.loads(event["body"])
        except Exception:
            return {}
    return {}


# ---------------------------------------------------------------------------
# Main Lambda Handler
# ---------------------------------------------------------------------------
# This function is called by AWS Lambda for each incoming request. 
# The complete API Gateway proxy request, including the HTTP method, path, query parameters, headers, and body, is contained in the `event` dict.
#  Runtime metadata is provided by the `context` object (not utilized here).
def lambda_handler(event, context):

    # --- Extract HTTP method ---
    # REST API (API Gateway v1) puts the method in event["httpMethod"].
    # HTTP API (API Gateway v2) puts it in event["requestContext"]["http"]["method"].
    # We check both so the same Lambda works with either API Gateway type.
    method = event.get("httpMethod")
    if not method:
        method = event.get("requestContext", {}).get("http", {}).get("method")

    # --- Extract request path ---
    # REST API uses event["path"]; HTTP API uses event["rawPath"].
    path = event.get("path")
    if not path:
        path = event.get("rawPath")

    # Strip the stage prefix (e.g. "/dev") so route matching works the same
    # regardless of which API Gateway stage the request came through.
    # Example: "/dev/login" becomes "/login".
    if path:
        path = path.replace("/dev", "")

    # Query string parameters (e.g. ?artist=Taylor+Swift&year=2008).
    # Default to empty dict so callers can safely use .get() without key errors.
    query = event.get("queryStringParameters") or {}

    # Parse the JSON request body (used by POST endpoints).
    body = get_body(event)

    # -------------------------------------------------------------------
    # OPTIONS — CORS Preflight
    # -------------------------------------------------------------------
    # Before sending a cross-origin POST or DELETE, browsers send an OPTIONS
    # "preflight" request to check whether the server allows it. We respond
    # immediately with 200 and the CORS headers (already in the response helper).
    if method == "OPTIONS":
        return response(200, {"message": "CORS OK"})

    # -------------------------------------------------------------------
    # POST /login
    # -------------------------------------------------------------------
    # Validates user-supplied credentials against the DynamoDB login table.
    # Uses GetItem (O(1) lookup) because email is the partition key — no scan needed.
    # Returns the user's email and user_name on success so the frontend can
    # display a welcome message without a second round-trip.
    if method == "POST" and path == "/login":
        email    = body.get("email")
        password = body.get("password")

        # GetItem retrieves a single item by its primary key.
        # result["Item"] is None if the email does not exist in the table.
        result = login_table.get_item(Key={"email": email})
        user   = result.get("Item")

        # Fail if the email is not found OR the password does not match.
        # Using the same vague message in both cases prevents an attacker from
        # learning which emails are registered (enumeration attack prevention).
        if not user or user.get("password") != password:
            return response(401, {
                "status":  "error",
                "message": "email or password is invalid"
            })

        # Credentials matched — return identity fields to the frontend session.
        return response(200, {
            "status":    "success",
            "email":     user["email"],
            "user_name": user["user_name"]
        })

    # -------------------------------------------------------------------
    # POST /register
    # -------------------------------------------------------------------
    # Creates a new user account in the login table.
    # Enforces uniqueness of email: each email may only be registered once.
    # Username is not required to be unique (per the assignment spec).
    if method == "POST" and path == "/register":
        email     = body.get("email")
        user_name = body.get("userName")  # Field name matches the frontend form key
        password  = body.get("password")

        # Validate that all required fields were provided before touching DynamoDB.
        if not email or not user_name or not password:
            return response(400, {
                "status":  "error",
                "message": "Please complete all fields"
            })

        # Check if the email is already registered using GetItem (exact key lookup).
        # A Scan is not needed here because email is the partition key of the login table.
        existing = login_table.get_item(Key={"email": email}).get("Item")

        if existing:
            # Assignment spec requires this exact error message wording.
            return response(400, {
                "status":  "error",
                "message": "The email already exists"
            })

        # Email is unique — store the new user. Passwords are stored in plain text
        # as permitted by the assignment spec for simplicity. In a real system,
        # passwords must always be salted and hashed (e.g. with bcrypt).
        login_table.put_item(Item={
            "email":     email,
            "user_name": user_name,
            "password":  password
        })

        return response(200, {
            "status":  "success",
            "message": "User registered successfully"
        })

    # -------------------------------------------------------------------
    # GET /query?title=&year=&artist=&album=
    # -------------------------------------------------------------------
   # Looks through the music table. While all four options are optional, at least #1 needs to be entered. 
   # AND logic is used to combine multiple parameters.
    # The most effective DynamoDB operation is used in the query routing scheme.
    # according to the qualities that match the indexes created for # the music table:
    # artist + year only -> ArtistYearLSI (LSI, artist partition key)
    # artist just -> Table query (partition key, no index required)
    # title (± artist) → TitleArtistGSI (GSI, title partition key)
    # album (± artist) → AlbumArtistGSI (GSI, album partition key)
    # artist + album -> AlbumArtistGSI (GSI, both keys used)
    # Anything else ⇒ Filter scan (fallback for year-only, etc.)
    #Because Query reads only the relevant partition rather than the full table, 
    # it is faster and less expensive (using fewer read capacity units) than Scan whenever possible.
    if method == "GET" and path == "/query":

        title  = query.get("title")
        year   = query.get("year")
        artist = query.get("artist")
        album  = query.get("album")

        # Reject the request if no search field was provided at all.
        if not title and not year and not artist and not album:
            return response(400, {
                "message": "Please enter at least one search field."
            })

        items = []  # Will hold the matching DynamoDB items

        # ------------------------------------------------------------------
        # Branch 1: artist + year  →  ArtistYearLSI
        # ------------------------------------------------------------------
        # The LSI (Local Secondary Index) shares the table's partition key
        # (artist) but uses year as its sort key, enabling an efficient
        # in-partition range/equality query by year without scanning all songs
        # for that artist. Example demo query: "Jimmy Buffett in 1974".
        if artist and year and not title and not album:
            try:
                result = music_table.query(
                    IndexName="ArtistYearLSI",
                    KeyConditionExpression=(
                        Key("artist").eq(artist) & Key("year").eq(int(year))
                    )
                )
                items = result.get("Items", [])
            except (ValueError, Exception):
                # int(year) would raise ValueError for non-numeric input;
                # fall through gracefully with an empty result.
                pass

        # ------------------------------------------------------------------
        # Branch 2: artist only  →  direct table Query on partition key
        # ------------------------------------------------------------------
        # No index is needed when the partition key (artist) is the only
        # search condition — DynamoDB can read exactly the right partition.
        # This is the most efficient possible lookup for an artist search.
        elif artist and not title and not album and not year:
            result = music_table.query(
                KeyConditionExpression=Key("artist").eq(artist)
            )
            items = result.get("Items", [])

        # ------------------------------------------------------------------
        # Branch 3: title (optionally + artist)  →  TitleArtistGSI
        # ------------------------------------------------------------------
        # title is not the table's partition key, so a GSI is required to
        # query by title without scanning the whole table.
        # If artist is also provided, it is added as a sort key condition on
        # the GSI, narrowing results further within the same partition.
        elif title and not album and not year:
            key_cond = Key("title").eq(title)
            if artist:
                key_cond = key_cond & Key("artist").eq(artist)
            result = music_table.query(
                IndexName="TitleArtistGSI",
                KeyConditionExpression=key_cond
            )
            items = result.get("Items", [])

        # ------------------------------------------------------------------
                # album only, or album + artist  →  Scan with contains() for partial matching
        # e.g. "Reputation" returns "Reputation" AND "Reputation (Deluxe)"
        # e.g. "Taylor Swift" + "Fearless" returns "Fearless" AND "Fearless (Platinum Edition)"
        elif album and not title and not year:
            filter_exp = Attr("album").contains(album)
            if artist:
                filter_exp = filter_exp & Attr("artist").contains(artist)
            result = music_table.scan(FilterExpression=filter_exp)
            items = result.get("Items", [])

        # artist + album  →  Scan with contains() for partial matching
        elif artist and album and not title and not year:
            filter_exp = (
                Attr("artist").contains(artist) &
                Attr("album").contains(album)
            )
            result = music_table.scan(FilterExpression=filter_exp)
            items = result.get("Items", [])

        # ------------------------------------------------------------------
        # Branch 6: Scan fallback
        # ------------------------------------------------------------------
  # Used for single-year searches and any combination of several fields not
        # covered by the previously indicated indexes (title + year + album, for instance).
        # After reading every item in the table, Scan applies the FilterExpression.
        # server-side. This is reasonable considering the dataset's small size (137 songs).
        # A production system with millions of songs would take its place.
        # utilizing other GSIs or search engines such as OpenSearch.
        else:
            filter_exp = None

            # Build the filter expression by ANDing each provided condition.
            # contains() is used instead of eq() for partial/substring matching,
            # so "Fearless" matches both "Fearless" and "Fearless (Platinum Edition)".
            if title:
                cond = Attr("title").contains(title)
                filter_exp = cond if filter_exp is None else filter_exp & cond

            if year:
                # year is stored as a Number in DynamoDB, so it must be cast to int.
                try:
                    cond = Attr("year").eq(int(year))
                except ValueError:
                    cond = Attr("year").eq(year)  # Fallback if non-numeric input
                filter_exp = cond if filter_exp is None else filter_exp & cond

            if artist:
                cond = Attr("artist").contains(artist)
                filter_exp = cond if filter_exp is None else filter_exp & cond

            if album:
                cond = Attr("album").contains(album)
                filter_exp = cond if filter_exp is None else filter_exp & cond

            result = music_table.scan(FilterExpression=filter_exp)
            items = result.get("Items", [])

        # Return the spec-required message if no songs matched the search.
        if not items:
            return response(200, {
                "message": "No result is retrieved. Please query again"
            })

        return response(200, items)

    # -------------------------------------------------------------------
    # POST /subscribe
    # -------------------------------------------------------------------
# Adds a song to the DynamoDB subscription list of the person who is now signed in.
    # Duplicate subscriptions are prevented via the composite key (email + song_id):
    # A 200 "exists" response indicates if the user has already subscribed to this music.

    #Rather than writing a duplicate item, # is returned.
    if method == "POST" and path == "/subscribe":
        email  = body.get("email")
        title  = body.get("title")
        artist = body.get("artist")
        year   = str(body.get("year"))  # Stored as String to stay consistent with song_id
        album  = body.get("album")

        # The frontend may send the image URL under different field names depending
        # on which version of the HTML is in use. Check all three variants in
        # priority order; fall back to an empty string if none is present.
        image_url = (
            body.get("imageUrl")   or   # camelCase field name (subscribe form)
            body.get("image_url")  or   # snake_case field name (query result passthrough)
            body.get("img_url")    or   # original field name from the JSON dataset
            ""
        )

        # Validate required fields before writing to DynamoDB.
        if not email or not title or not artist or not year:
            return response(400, {
                "status":  "error",
                "message": "Missing subscription details"
            })

        # song_id is the sort key of the subscription table.
        # Format: "title_year" (e.g. "Love Story_2008").
        # This matches the format used by RemoveSubscription so DELETEs work correctly.
        song_id = title + "_" + year

        # Check for an existing subscription using GetItem (exact key lookup).
        # This avoids overwriting an existing item with identical data.
        existing = subscription_table.get_item(
            Key={"email": email, "song_id": song_id}
        ).get("Item")

        if existing:
            return response(200, {
                "status":  "exists",
                "message": "Already subscribed"
            })

        # Write the new subscription item to DynamoDB.
        # image_url is stored here so the subscription area can display artist
        # images without querying the music table again on every page load.
        subscription_table.put_item(Item={
            "email":     email,
            "song_id":   song_id,
            "title":     title,
            "artist":    artist,
            "year":      year,
            "album":     album,
            "image_url": image_url
        })

        return response(200, {
            "status":  "success",
            "message": "Subscribed successfully"
        })

    # -------------------------------------------------------------------
    # GET /subscriptions?email=user@example.com
 # --------------------------------------------------- # Provides all of a user's subscriptions, which are used to fill the
    # After logging in, the "My Playlist" section appears on the home page.

    #Because email is the subscription table's partition key, all of a user's items reside in the same partition and are fetched in a single, effective transaction.
    if method == "GET" and path == "/subscriptions":
        email = query.get("email")

        if not email:
            return response(400, {
                "status":  "error",
                "message": "Email is required"
            })

        # KeyConditionExpression targets only the partition for this email,
        # returning all sort key values (all song_ids) for this user.
        result = subscription_table.query(
            KeyConditionExpression=Key("email").eq(email)
        )

        return response(200, result.get("Items", []))

    # -------------------------------------------------------------------
    # DELETE /subscription?email=user@example.com&songId=Love+Story_2008
    # -------------------------------------------------------------------
    # Removes a single subscription from the table when the user clicks
    # the "Remove" button. Uses DeleteItem with the full composite key
    # (email + song_id) for a precise, single-item delete — no scan needed.
    if method == "DELETE" and path == "/subscription":
        email   = query.get("email")
        song_id = query.get("songId")  # Passed as a URL query parameter from the frontend

        if not email or not song_id:
            return response(400, {
                "status":  "error",
                "message": "Email and songId are required"
            })

        # DeleteItem is idempotent — if the item does not exist, no error is raised.
        subscription_table.delete_item(
            Key={"email": email, "song_id": song_id}
        )

        return response(200, {
            "status":  "success",
            "message": "Subscription removed successfully"
        })

    # -------------------------------------------------------------------
    # 404 — No matching route
    # -------------------------------------------------------------------
  # Returned if none of the aforementioned handlers match the HTTP method + path combination. 
  # Since #API Gateway only forwards configured routes to this Lambda function, 
  # his shouldn't happen during regular operation.
    return response(404, {"message": "Invalid API route"})
