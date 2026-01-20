package com.example.eldercaremonitor.presentation

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text

data class EmergencyContact(
    val name: String,
    val phoneNumber: String
)

@Composable
fun EmergencyContactsScreen(
    contacts: List<EmergencyContact>,
    onCallContact: (EmergencyContact) -> Unit
) {
    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color(0xFF17233C)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        item {
            Text(
                text = "\uD83C\uDD98 Emergency Contacts",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
        }

        items(contacts) { contact ->
            Button(
                onClick = {
                    onCallContact(contact)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Call ${contact.name}",
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}