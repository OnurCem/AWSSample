package com.awstest;

import java.util.List;

/**
 * Created by Onur Cem on 5/26/2015.
 */
public interface GetImagesTaskListener {
    public void onComplete(List<Image> result);
}