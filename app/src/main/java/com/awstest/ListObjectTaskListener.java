package com.awstest;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import java.util.List;

/**
 * Created by Onur Cem on 3/25/2015.
 */
public interface ListObjectTaskListener {
    public void onComplete(List<Image> result);
}
