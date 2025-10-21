package com.example.kineticpulsemobileapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomeScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_screen);

        // Find the start button
        Button startButton = findViewById(R.id.startButton);

        // Set click listener for the start button
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to CharacterSelectionActivity when clicked
                Intent intent = new Intent(WelcomeScreen.this, CharacterSelectionActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Optional: Also allow clicking anywhere on the screen to proceed
        View rootLayout = findViewById(android.R.id.content);
        rootLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to CharacterSelectionActivity when clicked anywhere
                Intent intent = new Intent(WelcomeScreen.this, CharacterSelectionActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}