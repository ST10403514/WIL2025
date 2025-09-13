package com.example.kineticpulsemobileapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterScreen extends AppCompatActivity {

    private Button backToLoginBtn;
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button registerButton;
    private EditText usernameEditText;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_screen);

        backToLoginBtn = findViewById(R.id.btnBackToLogin);
        emailEditText = findViewById(R.id.regEmail);
        passwordEditText = findViewById(R.id.regPassword);
        usernameEditText = findViewById(R.id.regUsername);
        registerButton = findViewById(R.id.btncreateAccount);

        auth = FirebaseAuth.getInstance();

        backToLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent toLoginIntent = new Intent(RegisterScreen.this, LoginScreen.class);
                startActivity(toLoginIntent);
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(RegisterScreen.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                } else {
                    auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    saveRegisteredUsername();
                                    // Registration success
                                    Toast.makeText(RegisterScreen.this, "Registration successful", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(RegisterScreen.this, LoginScreen.class));
                                    finish();
                                } else {
                                    // Registration failed
                                    if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                        Toast.makeText(RegisterScreen.this, "Email is already registered", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(RegisterScreen.this, "Registration failed. Please try again later.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });
    }

    private void saveRegisteredUsername() {
        String username = usernameEditText.getText().toString();
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (uid != null) {
            RegisterRequest registerRequest = new RegisterRequest(uid, username);

            // Use RetrofitInstance to get the API handler
            RetrofitInstance.getApi().registerUser(registerRequest).enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(RegisterScreen.this, "Username saved to API", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(RegisterScreen.this, "Failed to save username to API", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    Toast.makeText(RegisterScreen.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

}
