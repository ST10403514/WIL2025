// Update app/src/main/java/com/example/kineticpulsemobileapp/SplashScreen.java
package com.example.kineticpulsemobileapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class SplashScreen extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        // After splash delay, go to WelcomeScreen instead of LoginScreen
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashScreen.this, WelcomeScreen.class);
            startActivity(intent);
            finish();
        }, 2000); // 2 seconds splash
    }
}