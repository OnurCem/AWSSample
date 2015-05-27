package com.awstest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
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
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;


/**
 * Created by Onur Cem on 3/19/2015.
 */
public class AWSController {
    private AmazonS3Client s3Client;
    private AmazonDynamoDBClient ddbClient;
    private DynamoDBMapper ddbMapper;
    private ListObjectTaskListener listObjectTaskListener;
    private SaveObjectTaskListener saveObjectTaskListener;
    private GetImagesTaskListener getImagesTaskListener;

    public AWSController() {
        createClients();
    }

    public void setListObjectTaskListener(ListObjectTaskListener listener) {
        listObjectTaskListener = listener;
    }

    public void setSaveObjectTaskListener(SaveObjectTaskListener listener) {
        saveObjectTaskListener = listener;
    }

    public void setGetImagesTaskListener(GetImagesTaskListener listener) {
        getImagesTaskListener = listener;
    }

    private void createClients() {
        s3Client = new AmazonS3Client(new BasicAWSCredentials("AKIAILUW66QTAMNCTWJQ",
                "L7Fnhuor8o/QGUHDX0tw6A411L8KLWYCGM/HiMs9"));

        ddbClient = new AmazonDynamoDBClient(new BasicAWSCredentials("AKIAILUW66QTAMNCTWJQ",
                "L7Fnhuor8o/QGUHDX0tw6A411L8KLWYCGM/HiMs9"));

        ddbClient.setRegion(Region.getRegion(Regions.US_WEST_2));
        ddbMapper = new DynamoDBMapper(ddbClient);
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

    public void likeImage(String imageId, String userId) {
        AsyncTaskHelper.execute(new LikeImageTask(), imageId, userId);
    }

    public void getImages(String bucketName) {
        AsyncTaskHelper.execute(new GetImagesTask(), bucketName);
    }

    public Image getObject(String bucketName, String key) {
        try {
            return new GetObjectTask().execute(bucketName, key).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void saveItem(String key, String values) {
        Map<String,AttributeValue> item = new HashMap<>();

        item.put("Image ID",
                new AttributeValue().withS(key));
        item.put("Hashtags",
                new AttributeValue().withS(values));

        AsyncTaskHelper.execute(new SaveItemTask(), item);
    }

    public String getHashtags(String imageId) {
        try {
            return new GetHashtagsTask().execute(imageId).get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String login(String username, String password) {
        try {
            return new LoginTask().execute(username, password).get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void executeSQLQuery(String sql) {
        AsyncTaskHelper.execute(new ExecuteSQLQueryTask(), sql);
    }

    public String executeSQLQuerySync(String sql) {
        try {
            return new ExecuteSQLQueryTask().execute(sql).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        }
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

    private class SaveObjectTask extends AsyncTask<InputStream, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(InputStream... params) {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

            ObjectMetadata metadata = new ObjectMetadata();
            PutObjectRequest por = new PutObjectRequest(MainActivity.BUCKET_NAME, timeStamp, params[0], metadata);

            PutObjectResult result = s3Client.putObject(por);

            return timeStamp;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (saveObjectTaskListener != null) {
                saveObjectTaskListener.onComplete(result);
            }
        }
    }

    private class GetObjectTask extends AsyncTask<String, Void, Image> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Image doInBackground(String... params) {
            Image image = new Image();
            S3Object s3Object = s3Client.getObject(params[0], params[1]);

            try {
                byte[] byteArray = IOUtils.toByteArray(s3Object.getObjectContent());
                //String imageId = s3Object.getObjectMetadata().getETag();
                String imageId = params[1];

                image.setBitmap(decodeSampledBitmapFromBytes(byteArray, 400, 150));
                image.setId(imageId);
                //image.setHashtags(getHashtags(imageId));
                Hashtag hashtag = ddbMapper.load(Hashtag.class, imageId);
                image.setHashtags(hashtag.getHashtags());

                try {
                    /*String row = executeSQLQuerySync("SELECT Username FROM USER, USER_IMAGE " +
                            "WHERE USER.Id = USER_IMAGE.UserId AND ImageId = '" + imageId + "'");
                    */
                    String sql = "SELECT Username FROM USER, USER_IMAGE " +
                            "WHERE USER.Id = USER_IMAGE.UserId AND ImageId = '" + imageId + "'";
                    sql = URLEncoder.encode(sql);

                    String row = new ServiceHandler().makeServiceCall(ConstantValues.MYSQL_SERVICE + sql,
                            ServiceHandler.GET);

                    image.setUsername(row.split("<br>")[1]);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            return image;
        }

        @Override
        protected void onPostExecute(Image result) {
            super.onPostExecute(result);
        }
    }

    private class ListObjectsTask extends AsyncTask<String, Void, List<Image>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<Image> doInBackground(String... params) {
            ObjectListing listing = s3Client.listObjects(params[0]);
            List<S3ObjectSummary> summaries = listing.getObjectSummaries();
            List<Image> result = new ArrayList<>();

            while (listing.isTruncated()) {
                listing = s3Client.listNextBatchOfObjects(listing);
                summaries.addAll(listing.getObjectSummaries());
            }

            for (S3ObjectSummary o : summaries) {
                Log.d("Object key", o.getKey());
                S3Object s3Object = s3Client.getObject(params[0], o.getKey());

                try {
                    Image image = new Image();
                    byte[] byteArray = IOUtils.toByteArray(s3Object.getObjectContent());
                    //String imageId = s3Object.getObjectMetadata().getETag();
                    String imageId = o.getKey();

                    image.setBitmap(decodeSampledBitmapFromBytes(byteArray, 400, 150));
                    image.setId(imageId);
                    image.setHashtags(getHashtags(imageId));

                    try {
                        String row = executeSQLQuerySync("SELECT Username FROM USER, USER_IMAGE " +
                                "WHERE USER.Id = USER_IMAGE.UserId AND ImageId = '" + imageId + "'");

                        image.setUsername(row.split("<br>")[1]);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    result.add(image);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(List<Image> result) {
            super.onPostExecute(result);
            if (listObjectTaskListener != null) {
                listObjectTaskListener.onComplete(result);
            }
        }
    }

    private class LikeImageTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            String queryResult = executeSQLQuerySync("INSERT INTO LIKE_IMAGE VALUES('" +
                    params[0] + "', " + params[1] + ")");

            return queryResult;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }
    }

    private class GetImagesTask extends AsyncTask<String, Void, List<Image>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<Image> doInBackground(String... params) {
            List<Image> result = new ArrayList<>();

            String queryResult = executeSQLQuerySync("SELECT ImageId FROM USER_IMAGE WHERE UserId IN " +
                    "(SELECT User2Id FROM FRIENDSHIP WHERE User1Id = " + ConstantValues.userId + ") UNION " +
                    "SELECT ImageId FROM USER_IMAGE WHERE UserId = " + ConstantValues.userId);

            try {
                String[] imageIds = queryResult.split("<br>");
                for (int i = 1; i < imageIds.length; i++) {
                    Image image = getObject(MainActivity.BUCKET_NAME, imageIds[i]);
                    result.add(image);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            return result;
        }

        @Override
        protected void onPostExecute(List<Image> result) {
            super.onPostExecute(result);
            if (getImagesTaskListener != null) {
                getImagesTaskListener.onComplete(result);
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
            PutItemRequest putItemRequest = new PutItemRequest("hashtag", params[0]);
            ddbClient.setRegion(Region.getRegion(Regions.US_WEST_2));
            PutItemResult result = ddbClient.putItem(putItemRequest);

            return result;
        }

        @Override
        protected void onPostExecute(PutItemResult result) {
            super.onPostExecute(result);
        }
    }

    private class GetHashtagsTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            Log.d("Image ID", params[0]);
            Hashtag result = ddbMapper.load(Hashtag.class, params[0]);
            return result.getHashtags();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }
    }

    private class LoginTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            ServiceHandler serviceHandler = new ServiceHandler();

            String sql = "SELECT Id, Username, FirstName, LastName FROM USER WHERE Username = '" + params[0] + "' AND Password = '" +
                    params[1] + "'";
            sql = URLEncoder.encode(sql);

            String result = serviceHandler.makeServiceCall(ConstantValues.MYSQL_SERVICE + sql,
                    ServiceHandler.GET);

            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }
    }

    private class ExecuteSQLQueryTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            ServiceHandler serviceHandler = new ServiceHandler();

            String sql = params[0];
            sql = URLEncoder.encode(sql);

            String result = serviceHandler.makeServiceCall(ConstantValues.MYSQL_SERVICE + sql,
                    ServiceHandler.GET);

            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }
    }

    private static Bitmap decodeSampledBitmapFromBytes(byte[] bytes,
                                                      int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
    }

    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
