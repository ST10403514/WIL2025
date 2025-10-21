package com.example.kineticpulsemobileapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class CharacterSelectionActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "KineticPulsePrefs";
    private static final String SELECTED_CHARACTER = "selected_character";

    private ImageView lionImage, boyImage, girlImage;
    private CardView lionCard, boyCard, girlCard;
    private String selectedCharacter = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_character_selection);

        initializeViews();
        setupClickListeners();
        loadPreviousSelection();
    }

    private void initializeViews() {
        lionImage = findViewById(R.id.lion_image);
        boyImage = findViewById(R.id.boy_image);
        girlImage = findViewById(R.id.girl_image);

        lionCard = findViewById(R.id.lionCard);
        boyCard = findViewById(R.id.boyCard);
        girlCard = findViewById(R.id.girlCard);
    }

    private void setupClickListeners() {
        // Set click listeners for both the images and the cards
        lionImage.setOnClickListener(v -> selectCharacter("lion", lionCard));
        boyImage.setOnClickListener(v -> selectCharacter("boy", boyCard));
        girlImage.setOnClickListener(v -> selectCharacter("girl", girlCard));

        lionCard.setOnClickListener(v -> selectCharacter("lion", lionCard));
        boyCard.setOnClickListener(v -> selectCharacter("boy", boyCard));
        girlCard.setOnClickListener(v -> selectCharacter("girl", girlCard));

        Button confirmButton = findViewById(R.id.confirm_button);
        confirmButton.setOnClickListener(v -> confirmSelection());
    }

    private void selectCharacter(String character, CardView selectedCard) {
        // Reset all card selections
        resetCardSelection();

        // Highlight selected card with a glowing effect
        selectedCard.setCardElevation(16f);
        selectedCard.setCardBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));

        // Add a border to show selection
        selectedCard.setBackgroundResource(R.drawable.character_selected_border);

        selectedCharacter = character;
    }

    private void resetCardSelection() {
        // Reset lion card
        if (lionCard != null) {
            lionCard.setCardElevation(8f);
            lionCard.setCardBackgroundColor(0xFFF7DC6F); // Golden yellow
            lionCard.setBackgroundResource(0);
        }

        // Reset boy card
        if (boyCard != null) {
            boyCard.setCardElevation(8f);
            boyCard.setCardBackgroundColor(0xFF3498DB); // Blue
            boyCard.setBackgroundResource(0);
        }

        // Reset girl card
        if (girlCard != null) {
            girlCard.setCardElevation(8f);
            girlCard.setCardBackgroundColor(0xFF9B59B6); // Purple
            girlCard.setBackgroundResource(0);
        }
    }

    private void confirmSelection() {
        if (!selectedCharacter.isEmpty()) {
            // Save selection to SharedPreferences
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            prefs.edit().putString(SELECTED_CHARACTER, selectedCharacter).apply();

            // Start MainActivity with character selection
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("selected_character", selectedCharacter);
            startActivity(intent);
            finish();
        }
    }

    private void loadPreviousSelection() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String previousSelection = prefs.getString(SELECTED_CHARACTER, "");

        if (!previousSelection.isEmpty()) {
            switch (previousSelection) {
                case "lion":
                    selectCharacter("lion", lionCard);
                    break;
                case "boy":
                    selectCharacter("boy", boyCard);
                    break;
                case "girl":
                    selectCharacter("girl", girlCard);
                    break;
            }
        }
    }

    public static String getSelectedCharacter(SharedPreferences prefs) {
        return prefs.getString(SELECTED_CHARACTER, "lion"); // default to lion
    }
}