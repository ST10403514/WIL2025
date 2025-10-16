package com.example.kineticpulsemobileapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.concurrent.Executor;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginScreen extends AppCompatActivity {
    private static final int RC_SIGN_IN = 9001;

    private Button regPageBtn;
    private Button loginButton;
    private Button btnGoogleSignIn;
    private EditText emailEditText;
    private EditText passwordEditText;

    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;

    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private Executor executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_screen);

        auth = FirebaseAuth.getInstance();

        // Google Sign-In configuration (using auto-generated client ID)
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getWebClientId())
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize views
        regPageBtn = findViewById(R.id.btnRegPage);
        loginButton = findViewById(R.id.btnLogin);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        emailEditText = findViewById(R.id.logEmail);
        passwordEditText = findViewById(R.id.logPassword);
        
        // Initialize logo and start pulse animation
        ImageView logoImageView = findViewById(R.id.logoImageView);
        Animation pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_animation);
        logoImageView.startAnimation(pulseAnimation);

        // Biometric Authentication setup
        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                signInWithGoogle();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(LoginScreen.this, "Biometric authentication failed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(LoginScreen.this, "Biometric error: " + errString, Toast.LENGTH_SHORT).show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Authentication")
                .setSubtitle("Authenticate using your fingerprint")
                .setNegativeButtonText("Cancel")
                .build();

        // Check for biometric capability
        BiometricManager biometricManager = BiometricManager.from(this);
        btnGoogleSignIn.setOnClickListener(v -> {
            if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG
                    | BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) {
                biometricPrompt.authenticate(promptInfo);
            } else {
                signInWithGoogle();
            }
        });

        // Registration page clicked
        regPageBtn.setOnClickListener(v -> {
            Intent regIntent = new Intent(LoginScreen.this, RegisterScreen.class);
            startActivity(regIntent);
        });

        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginScreen.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            } else {
                auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(LoginScreen.this, "Login successful", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginScreen.this, MainActivity.class));
                                finish();
                            } else {
                                Toast.makeText(LoginScreen.this, "Login failed. Please try again.", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            firebaseAuthWithGoogle(account);
        } catch (ApiException e) {
            Log.w("LoginScreen", "Google sign-in failed: " + e.getStatusCode());
            Toast.makeText(this, "Google Sign-In failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(account.getIdToken(), null))
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        saveUsernameToDatabase(account.getDisplayName(), account);
                    } else {
                        Toast.makeText(LoginScreen.this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUsernameToDatabase(String username, GoogleSignInAccount account) {
        String currentUserUid = auth.getCurrentUser().getUid();

        RegisterRequest registerRequest = new RegisterRequest(username, currentUserUid);
        RetrofitInstance.getApi().registerUser(registerRequest).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful()) {
                    startActivity(new Intent(LoginScreen.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(LoginScreen.this, "Failed to save username to API", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Toast.makeText(LoginScreen.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getWebClientId() {
        // Uses the client ID from google-services.json via strings.xml
        return getString(R.string.default_web_client_id);
    }
}
