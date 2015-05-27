package com.awstest;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;

/**
 * Created by Onur Cem on 5/15/2015.
 */
@DynamoDBTable(tableName = "hashtag")
public class Hashtag {
    private String imageId;
    private String hashtags;

    @DynamoDBHashKey(attributeName = "Image ID")
    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    @DynamoDBAttribute(attributeName = "Hashtags")
    public String getHashtags() {
        return hashtags;
    }

    public void setHashtags(String hashtags) {
        this.hashtags = hashtags;
    }
}
