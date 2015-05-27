package com.awstest;

import com.amazonaws.services.s3.model.PutObjectResult;

/**
 * Created by Onur Cem on 3/26/2015.
 */
public interface SaveObjectTaskListener {
    public void onComplete(String objectKey);
}
