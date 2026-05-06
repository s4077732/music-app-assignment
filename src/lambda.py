# Adapted from Practical Exercise 4 lambda_function_updated.py
# Modified for Music App Assignment.
# Handles login, register, query, subscribe, get subscriptions, and remove.

import json
import boto3
from decimal import Decimal
from boto3.dynamodb.conditions import Attr

# Connect to DynamoDB in us-east-1
dynamodb = boto3.resource("dynamodb", region_name="us-east-1")

# DynamoDB tables
login_table = dynamodb.Table("login")
music_table = dynamodb.Table("music")
subscription_table = dynamodb.Table("subscription")


# Convert Decimal values from DynamoDB into normal JSON numbers
def decimal_default(obj):
    if isinstance(obj, Decimal):
        return int(obj)
    raise TypeError


# Standard response with CORS headers for API Gateway
def response(status_code, body):
    return {
        "statusCode": status_code,
        "headers": {
            "Access-Control-Allow-Headers": "Content-Type",
            "Access-Control-Allow-Origin": "*",
            "Access-Control-Allow-Methods": "GET,POST,DELETE,OPTIONS"
        },
        "body": json.dumps(body, default=decimal_default)
    }


# Parse JSON body safely
def get_body(event):
    if event.get("body"):
        return json.loads(event["body"])
    return {}


# Format artist name for S3 image URL
def format_artist_name(artist):
    safe = ""
    previous_underscore = False

    for ch in artist:
        if ch.isalnum():
            safe += ch
            previous_underscore = False
        else:
            if not previous_underscore:
                safe += "_"
                previous_underscore = True

    return safe.strip("_")


# Main Lambda handler
def lambda_handler(event, context):


    # Supports both REST API format and HTTP API format
    method = event.get("httpMethod")

    if not method:
        method = event.get("requestContext", {}).get("http", {}).get("method")

    path = event.get("path")

    if not path:
        path = event.get("rawPath")

    # Remove stage name if present
    if path:
        path = path.replace("/dev", "")
    query = event.get("queryStringParameters") or {}
    body = get_body(event)

    # Browser CORS preflight request
    if method == "OPTIONS":
        return response(200, {"message": "CORS OK"})

    # POST /login
    if method == "POST" and path == "/login":
        email = body.get("email")
        password = body.get("password")

        result = login_table.get_item(Key={"email": email})
        user = result.get("Item")

        if not user or user.get("password") != password:
            return response(401, {
                "status": "error",
                "message": "email or password is invalid"
            })

        return response(200, {
            "status": "success",
            "email": user["email"],
            "user_name": user["user_name"]
        })

    # POST /register
    if method == "POST" and path == "/register":
        email = body.get("email")
        user_name = body.get("userName")
        password = body.get("password")

        existing = login_table.get_item(Key={"email": email}).get("Item")

        if existing:
            return response(400, {
                "status": "error",
                "message": "The email already exists"
            })

        login_table.put_item(Item={
            "email": email,
            "user_name": user_name,
            "password": password
        })

        return response(200, {
            "status": "success",
            "message": "User registered successfully"
        })

    # GET /query?artist=Taylor Swift&album=Fearless
    if method == "GET" and path == "/query":

        title = query.get("title")
        year = query.get("year")
        artist = query.get("artist")
        album = query.get("album")

        if not title and not year and not artist and not album:
            return response(400, {
                "message": "Please enter at least one search field."
            })

        filter_exp = None

        if title:
            condition = Attr("title").eq(title)
            filter_exp = condition if filter_exp is None else filter_exp & condition

        if year:
            condition = Attr("year").eq(int(year))
            filter_exp = condition if filter_exp is None else filter_exp & condition

        if artist:
            condition = Attr("artist").eq(artist)
            filter_exp = condition if filter_exp is None else filter_exp & condition

        if album:
            condition = Attr("album").eq(album)
            filter_exp = condition if filter_exp is None else filter_exp & condition

        result = music_table.scan(FilterExpression=filter_exp)
        items = result.get("Items", [])

        if not items:
            return response(200, {
                "message": "No result is retrieved. Please query again"
            })

        return response(200, items)

    # POST /subscribe
    if method == "POST" and path == "/subscribe":
        email = body.get("email")
        title = body.get("title")
        artist = body.get("artist")
        year = str(body.get("year"))
        album = body.get("album")

        song_id = title + "_" + year

        safe_artist = format_artist_name(artist)
        image_url = "https://music-application-img-upload.s3.amazonaws.com/" + safe_artist + ".jpg"

        existing = subscription_table.get_item(
            Key={
                "email": email,
                "song_id": song_id
            }
        ).get("Item")

        if existing:
            return response(200, {
                "status": "exists",
                "message": "Already subscribed"
            })

        subscription_table.put_item(Item={
            "email": email,
            "song_id": song_id,
            "title": title,
            "artist": artist,
            "year": year,
            "album": album,
            "image_url": image_url
        })

        return response(200, {
            "status": "success",
            "message": "Subscribed successfully"
        })

    # GET /subscriptions?email=...
    if method == "GET" and path == "/subscriptions":
        email = query.get("email")

        result = subscription_table.query(
            KeyConditionExpression=boto3.dynamodb.conditions.Key("email").eq(email)
        )

        return response(200, result.get("Items", []))

    # DELETE /subscription?email=...&songId=Love Story_2008
    if method == "DELETE" and path == "/subscription":
        email = query.get("email")
        song_id = query.get("songId")

        subscription_table.delete_item(
            Key={
                "email": email,
                "song_id": song_id
            }
        )

        return response(200, {
            "status": "success",
            "message": "Subscription removed successfully"
        })

    return response(404, {
        "message": "Invalid API route"
    })
