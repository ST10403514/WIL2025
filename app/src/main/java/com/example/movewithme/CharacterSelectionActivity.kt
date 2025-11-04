package com.example.movewithme

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class CharacterSelectionActivity : ComponentActivity() {

    companion object {
        private const val PREFS_NAME = "MoveWithMePrefs"
        private const val SELECTED_CHARACTER = "selected_character"

        fun getSelectedCharacter(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(SELECTED_CHARACTER, "lion") ?: "lion"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load previous selection
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val previousSelection = prefs.getString(SELECTED_CHARACTER, "")

        setContent {
            CharacterSelectionScreen(
                initialSelection = previousSelection ?: "",
                onCharacterSelected = { character ->
                    // Save selection
                    prefs.edit().putString(SELECTED_CHARACTER, character).apply()

                    // Start MainActivity
                    val intent = Intent(this, MainActivity::class.java).apply {
                        putExtra("selected_character", character)
                    }
                    startActivity(intent)
                    finish()
                }
            )
        }
    }
}

@Composable
fun CharacterSelectionScreen(
    initialSelection: String = "",
    onCharacterSelected: (String) -> Unit
) {
    var selectedCharacter by remember { mutableStateOf(initialSelection) }
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFE5B4))
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(25.dp)),
                shape = RoundedCornerShape(25.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFF6B35))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo placeholder (using R.drawable.pulse_logo if available)
                    // For now, just text
                    Text(
                        text = "⚡",
                        fontSize = 64.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Choose Your Character",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Select your movement companion",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Character Selection Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(25.dp)),
                shape = RoundedCornerShape(25.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Character Grid
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 30.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Lion Character
                        CharacterCard(
                            character = "lion",
                            name = "Leo the Lion",
                            description = "Strong & Bold",
                            backgroundColor = Color(0xFFF7DC6F),
                            isSelected = selectedCharacter == "lion",
                            imageRes = R.drawable.character_lion,
                            onSelect = { selectedCharacter = "lion" }
                        )

                        // Boy Character
                        CharacterCard(
                            character = "boy",
                            name = "Max the Explorer",
                            description = "Quick & Agile",
                            backgroundColor = Color(0xFF3498DB),
                            isSelected = selectedCharacter == "boy",
                            imageRes = R.drawable.character_boy,
                            onSelect = { selectedCharacter = "boy" }
                        )

                        // Girl Character
                        CharacterCard(
                            character = "girl",
                            name = "Luna the Dancer",
                            description = "Graceful & Energetic",
                            backgroundColor = Color(0xFF9B59B6),
                            isSelected = selectedCharacter == "girl",
                            imageRes = R.drawable.character_girl,
                            onSelect = { selectedCharacter = "girl" }
                        )
                    }

                    // Instructions
                    Text(
                        text = "Tap on a character to select your movement companion. Each character has unique animations!",
                        fontSize = 14.sp,
                        color = Color(0xFF2D3748),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                    )

                    // Confirm Button
                    Button(
                        onClick = {
                            if (selectedCharacter.isNotEmpty()) {
                                onCharacterSelected(selectedCharacter)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(65.dp)
                            .shadow(6.dp, RoundedCornerShape(4.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2ECC71)
                        ),
                        shape = RoundedCornerShape(4.dp),
                        enabled = selectedCharacter.isNotEmpty()
                    ) {
                        Text(
                            text = "START THE JOURNEY!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7DC6F))
            ) {
                Text(
                    text = "Your adventure awaits!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D3748),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun CharacterCard(
    character: String,
    name: String,
    description: String,
    backgroundColor: Color,
    isSelected: Boolean,
    imageRes: Int,
    onSelect: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .size(100.dp)
                .clickable { onSelect() }
                .then(
                    if (isSelected) {
                        Modifier
                            .shadow(16.dp, RoundedCornerShape(25.dp))
                            .border(4.dp, Color(0xFFFF9800), RoundedCornerShape(25.dp))
                    } else {
                        Modifier.shadow(8.dp, RoundedCornerShape(25.dp))
                    }
                ),
            shape = RoundedCornerShape(25.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) Color(0xFFFFA726) else backgroundColor
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isSelected) Color(0xFFFFA726) else backgroundColor)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = "$name character",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2D3748),
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )

        Text(
            text = description,
            fontSize = 11.sp,
            color = Color(0xFFFF6B35),
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}
