// app/src/main/java/com/smartparking/activities/ForgotPasswordActivity.java
package com.smartparking.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.smartparking.R;
import com.smartparking.viewmodels.AuthViewModel;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText editTextEmail;
    private Button buttonResetPassword;
    private ImageView imageViewBack;
    private ProgressBar progressBar;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Initialize views
        editTextEmail = findViewById(R.id.editTextEmail);
        buttonResetPassword = findViewById(R.id.buttonResetPassword);
        imageViewBack = findViewById(R.id.imageViewBack);
        progressBar = findViewById(R.id.progressBar);

        // Initialize ViewModel
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Set click listeners
        buttonResetPassword.setOnClickListener(v -> resetPassword());

        imageViewBack.setOnClickListener(v -> onBackPressed());
    }

    private void resetPassword() {
        String email = editTextEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required");
            editTextEmail.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        authViewModel.resetPassword(email);

        Toast.makeText(this, "Password reset email sent. Check your inbox.", Toast.LENGTH_LONG).show();

        progressBar.setVisibility(View.GONE);

        // Go back to login screen
        onBackPressed();
    }
}