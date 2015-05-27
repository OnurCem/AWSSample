package com.awstest;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import java.util.Arrays;


/**
 * A simple {@link Fragment} subclass.
 */
public class AddHashtagFragment extends Fragment {

    private AWSController awsCtrl;
    private String objectKey;

    public AddHashtagFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        awsCtrl = new AWSController();
        Bundle bundle = this.getArguments();
        objectKey = bundle.getString("objectKey");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_add_hashtag, container, false);

        final EditText editText = (EditText) rootView.findViewById(R.id.hashtags_edittext);
        Button share = (Button) rootView.findViewById(R.id.save_hashtags_button);

        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String hashtags = editText.getText().toString();
                awsCtrl.saveItem(objectKey, hashtags);
                awsCtrl.executeSQLQuery("INSERT INTO USER_IMAGE VALUES(" + ConstantValues.userId +
                    ", '" + objectKey + "')");
                awsCtrl.listObjects(MainActivity.BUCKET_NAME);
                closeFragment();
            }
        });

        return rootView;
    }

    private void closeFragment() {
        getFragmentManager().beginTransaction()
                .replace(R.id.container, new MainFragment())
                .commit();
    }
}
