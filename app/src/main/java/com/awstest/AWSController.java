package com.awstest;

import android.os.AsyncTask;
import android.util.Log;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.IOUtils;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by Onur Cem on 3/19/2015.
 */
public class AWSController {
    private AmazonS3Client s3Client;
    private AmazonDynamoDBClient ddbClient;
    private ListObjectTaskListener listObjectTaskListener;
    private SaveObjectTaskListener saveObjectTaskListener;

    public AWSController() {
        createClients();
    }

    public void setListObjectTaskListener(ListObjectTaskListener listener) {
        listObjectTaskListener = listener;
    }

    public void setSaveObjectTaskListener(SaveObjectTaskListener listener) {
        saveObjectTaskListener = listener;
    }

    private void createClients() {
        s3Client = new AmazonS3Client(new BasicAWSCredentials("AKIAILUW66QTAMNCTWJQ",
                "L7Fnhuor8o/QGUHDX0tw6A411L8KLWYCGM/HiMs9"));

        ddbClient = new AmazonDynamoDBClient(new BasicAWSCredentials("AKIAILUW66QTAMNCTWJQ",
                "L7Fnhuor8o/QGUHDX0tw6A411L8KLWYCGM/HiMs9"));
    }

    public void createBucket(String bucketName) {
        AsyncTaskHelper.execute(new CreateBucketTask(), bucketName);
    }

    public void saveObject(InputStream is) {
        AsyncTaskHelper.execute(new SaveObjectTask(), is);
    }

    public void listObjects(String bucketName) {
        AsyncTaskHelper.execute(new ListObjectsTask(), bucketName);
    }

    public void saveItem(String key, List<String> values) {
        AsyncTaskHelper.execute(new SaveItemTask(), null);
    }

    private class CreateBucketTask extends AsyncTask<String, Void, Bucket> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Bucket doInBackground(String... params) {
            Bucket bucket = s3Client.createBucket(params[0]);
            return bucket;
        }

        @Override
        protected void onPostExecute(Bucket result) {
            super.onPostExecute(result);
        }
    }

    private class SaveObjectTask extends AsyncTask<InputStream, Void, PutObjectResult> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected PutObjectResult doInBackground(InputStream... params) {
            //PutObjectRequest por = new PutObjectRequest("onurcemsenel", "deadline_dev",
                    //new java.io.File(params[0]));
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

            ObjectMetadata metadata = new ObjectMetadata();
            PutObjectRequest por = new PutObjectRequest("onurcemsenel", timeStamp, params[0], metadata);

            PutObjectResult result = s3Client.putObject(por);

            return result;
        }

        @Override
        protected void onPostExecute(PutObjectResult result) {
            super.onPostExecute(result);
            if (saveObjectTaskListener != null) {
                saveObjectTaskListener.onComplete(result);
            }
        }
    }

    private class ListObjectsTask extends AsyncTask<String, Void, List<byte[]>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<byte[]> doInBackground(String... params) {
            ObjectListing listing = s3Client.listObjects(params[0]);
            List<S3ObjectSummary> summaries = listing.getObjectSummaries();
            List<byte[]> result = new ArrayList<>();

            while (listing.isTruncated()) {
                listing = s3Client.listNextBatchOfObjects(listing);
                summaries.addAll(listing.getObjectSummaries());
            }

            for (S3ObjectSummary o : summaries) {
                Log.d("Object key", o.getKey());
                S3Object s3Object = s3Client.getObject(params[0], o.getKey());

                try {
                    byte[] byteArray = IOUtils.toByteArray(s3Object.getObjectContent());
                    result.add(byteArray);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(List<byte[]> result) {
            super.onPostExecute(result);
            if (listObjectTaskListener != null) {
                listObjectTaskListener.onComplete(result);
            }
        }
    }

    private class SaveItemTask extends AsyncTask<Map<String,AttributeValue>, Void, PutItemResult> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected PutItemResult doInBackground(Map<String,AttributeValue>... params) {
            Map<String,AttributeValue> item = new HashMap<>();
            item.put("Image ID",
                    new AttributeValue().withS("1234"));
            item.put("Hashtags",
                    new AttributeValue().withS("Green"));

            PutItemRequest putItemRequest = new PutItemRequest("hashtag", item);
            ddbClient.setRegion(Region.getRegion(Regions.US_WEST_2));
            PutItemResult result = ddbClient.putItem(putItemRequest);

            return result;
        }

        @Override
        protected void onPostExecute(PutItemResult result) {
            super.onPostExecute(result);

        }
    }
}
