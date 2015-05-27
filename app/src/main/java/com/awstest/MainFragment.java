package com.awstest;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;

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
    implements ListObjectTaskListener, SaveObjectTaskListener, GetImagesTaskListener {

    private static final int REQUEST_TAKE_PHOTO = 1;
    private String mCurrentPhotoPath;
    private Uri imageUri;
    private ListView imageListView;
    private Button takePhotoButton;
    private TextView usernameText;
    private AWSController awsCtrl;
    private ProgressDialog progressDialog;

    public MainFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        awsCtrl = new AWSController();
        awsCtrl.setListObjectTaskListener(this);
        awsCtrl.setSaveObjectTaskListener(this);
        awsCtrl.setGetImagesTaskListener(this);
        //awsCtrl.listObjects(MainActivity.BUCKET_NAME);
        awsCtrl.getImages(MainActivity.BUCKET_NAME);
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setTitle("Loading");
        progressDialog.setMessage("Please wait...");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        imageListView = (ListView) rootView.findViewById(R.id.image_list);
        takePhotoButton = (Button) rootView.findViewById(R.id.take_photo_button);
        usernameText = (TextView) rootView.findViewById(R.id.username_text);

        usernameText.setText(ConstantValues.userFullname);

        takePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        progressDialog.show();

        return rootView;
    }

    public void loadImages(List<Image> images) {
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
    public void onComplete(String objectKey) {
        AddHashtagFragment addHashtagFragment = new AddHashtagFragment();
        Bundle bundle = new Bundle();
        bundle.putString("objectKey", objectKey);
        addHashtagFragment.setArguments(bundle);

        getFragmentManager().beginTransaction()
                .replace(R.id.container, addHashtagFragment)
                .commit();
    }

    @Override
    public void onComplete(List<Image> result) {
        loadImages(result);
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
