package com.awstest;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;

/**
 * Created by Onur Cem on 7/4/2014.
 */
public class AsyncTaskHelper {

    public static <P, T extends AsyncTask<P, ?, ?>> void execute(T task) {
        execute(task, (P[]) null);
    }

    @SuppressLint("NewApi")
    public static <P, T extends AsyncTask<P, ?, ?>> void execute(T task, P... params) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        } else {
            task.execute(params);
        }
    }

}
