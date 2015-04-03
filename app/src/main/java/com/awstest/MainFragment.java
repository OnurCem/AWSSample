package com.awstest;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import com.amazonaws.services.s3.model.PutObjectResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Onur Cem on 3/25/2015.
 */
public class MainFragment extends Fragment
    implements ListObjectTaskListener, SaveObjectTaskListener {

    private static final String BUCKET_NAME = "onurcemsenel";
    private static final int REQUEST_TAKE_PHOTO = 1;
    private String mCurrentPhotoPath;
    private Uri imageUri;
    private ListView imageListView;
    private Button takePhotoButton;
    private AWSController awsCtrl;

    public MainFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        awsCtrl = new AWSController();
        awsCtrl.setListObjectTaskListener(this);
        awsCtrl.setSaveObjectTaskListener(this);
        awsCtrl.listObjects(BUCKET_NAME);
        awsCtrl.saveItem("", null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        imageListView = (ListView) rootView.findViewById(R.id.image_list);
        takePhotoButton = (Button) rootView.findViewById(R.id.take_photo_button);

        takePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        return rootView;
    }

    public void loadImages(List<Bitmap> images) {
        ImageListAdapter adapter = new ImageListAdapter(getActivity(), R.id.image_list, images);
        imageListView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                imageUri = Uri.fromFile(photoFile);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    public static Bitmap decodeSampledBitmapFromBytes(byte[] bytes,
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

    public static int calculateInSampleSize(
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            try {
                Bitmap image = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                image.compress(Bitmap.CompressFormat.JPEG, 25, bos);
                byte[] bitmapData = bos.toByteArray();
                ByteArrayInputStream bs = new ByteArrayInputStream(bitmapData);
                awsCtrl.saveObject(bs);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onComplete(PutObjectResult result) {
        awsCtrl.listObjects(BUCKET_NAME);
    }

    @Override
    public void onComplete(List<byte[]> result) {
        List<Bitmap> images = new ArrayList<>();

        for (byte[] imageBytes : result) {
            images.add(decodeSampledBitmapFromBytes(imageBytes, 400, 150));
        }
        loadImages(images);
    }
}
