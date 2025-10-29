package com.example.kineticpulsemobileapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.ViewOutlineProvider;
import androidx.core.content.ContextCompat;

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
        resetCardSelection();

        // Keep original color
        selectedCard.setCardBackgroundColor(selectedCard.getCardBackgroundColor().getDefaultColor());

        // Apply THICK PURPLE BORDER using Outline
        selectedCard.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                int radius = 25; // Match cardCornerRadius
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        selectedCard.setClipToOutline(true);

        // Add elevation + scale
        selectedCard.setCardElevation(24f);
        selectedCard.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).start();

        // Draw border using Canvas (via LayerDrawable)
        Drawable border = ContextCompat.getDrawable(this, R.drawable.character_selected_border);
        Drawable background = selectedCard.getBackground();
        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{background, border});
        selectedCard.setBackground(layerDrawable);

        selectedCharacter = character;
    }

    private void resetCardSelection() {
        CardView[] cards = {lionCard, boyCard, girlCard};
        int[] defaultColors = {0xFFF7DC6F, 0xFF3498DB, 0xFF9B59B6};

        for (int i = 0; i < cards.length; i++) {
            CardView card = cards[i];
            if (card != null) {
                card.setCardElevation(8f);
                card.setCardBackgroundColor(defaultColors[i]);
                card.setBackgroundResource(0); // Remove layered drawable
                card.setOutlineProvider(null);
                card.setClipToOutline(false);
                card.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
            }
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