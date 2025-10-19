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
    private Button googleSignInBtn;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private Button registerButton;
    private EditText usernameEditText;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_screen);

        // Initialize views with correct IDs from new layout
        backToLoginBtn = findViewById(R.id.btnBackToLogin);
        googleSignInBtn = findViewById(R.id.btnGoogleSignIn);
        emailEditText = findViewById(R.id.regEmail);
        passwordEditText = findViewById(R.id.regPassword);
        confirmPasswordEditText = findViewById(R.id.regConfirmPassword);
        usernameEditText = findViewById(R.id.regUsername);
        registerButton = findViewById(R.id.btnRegister);  // FIXED: Changed from btncreateAccount

        auth = FirebaseAuth.getInstance();

        // Back to Login Button
        backToLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent toLoginIntent = new Intent(RegisterScreen.this, LoginScreen.class);
                startActivity(toLoginIntent);
                finish();
            }
        });

        // Google Sign In Button (placeholder - implement Google Sign-In if needed)
        googleSignInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(RegisterScreen.this, "Google Sign-In coming soon! 🎉", Toast.LENGTH_SHORT).show();
                // TODO: Implement Google Sign-In functionality
            }
        });

        // Register Button
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString().trim();
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();
                String confirmPassword = confirmPasswordEditText.getText().toString().trim();

                // Validation
                if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(RegisterScreen.this, "Please fill in all fields 📝", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    Toast.makeText(RegisterScreen.this, "Passwords don't match! 🔒", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.length() < 6) {
                    Toast.makeText(RegisterScreen.this, "Password must be at least 6 characters 🔐", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Create Firebase account
                auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                saveRegisteredUsername();
                                Toast.makeText(RegisterScreen.this, "Registration successful! 🎉", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(RegisterScreen.this, LoginScreen.class));
                                finish();
                            } else {
                                if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                    Toast.makeText(RegisterScreen.this, "Email is already registered 📧", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(RegisterScreen.this, "Registration failed. Please try again 😔", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }

    private void saveRegisteredUsername() {
        String username = usernameEditText.getText().toString();
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (uid != null) {
            RegisterRequest registerRequest = new RegisterRequest(username, uid);

            RetrofitInstance.getApi().registerUser(registerRequest).enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(RegisterScreen.this, "Username saved! ✅", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(RegisterScreen.this, "Failed to save username ⚠️", Toast.LENGTH_SHORT).show();
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