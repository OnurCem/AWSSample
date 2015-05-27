package com.awstest;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.net.URLEncoder;


public class LoginActivity extends ActionBarActivity {
    private String username;
    private String password;
    private EditText editUsername;
    private EditText editPassword;
    private Button loginButton;

    public LoginActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editUsername = (EditText) findViewById(R.id.username_edittext);
        editPassword = (EditText) findViewById(R.id.password_edittext);
        editPassword.setTransformationMethod(new PasswordTransformationMethod());
        loginButton = (Button) findViewById(R.id.login_button);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                username = editUsername.getText().toString();
                password = editPassword.getText().toString();

                if (!username.isEmpty() && !password.isEmpty()) {
                    login(username, password);
                } else {
                    Toast.makeText(LoginActivity.this, "Please, enter your username and password",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void login(String username, String password) {
        AWSController awsController = new AWSController();
        String result = awsController.login(username, password);

        try {
            String[] rows = result.split("<br>");
            ConstantValues.userId = rows[1].split(",")[0];
            ConstantValues.username = rows[1].split(",")[1];
            ConstantValues.userFullname = rows[1].split(",")[2] + " " + rows[1].split(",")[3];

            Intent i = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(i);
            finish();
        } catch (Exception ex) {
            Toast.makeText(LoginActivity.this, "Try again",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
