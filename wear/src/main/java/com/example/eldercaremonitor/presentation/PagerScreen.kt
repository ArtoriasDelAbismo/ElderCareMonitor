package com.example.eldercaremonitor.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagerScreen(
    heartRate: Int?,
    contacts: List<EmergencyContact>,
    onPanic: () -> Unit,
    wearingStatus: String,
    onCallContact: (EmergencyContact) -> Unit,
    onDebugFall: () -> Unit,
    onDangerousHrSuggestion: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 2 }
    )

    val scope = rememberCoroutineScope()

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        when (page) {

            // Page 0 → Main screen
            0 -> HeartRateScreen(
                hr = heartRate,
                onPanic = onPanic,
                onDebugFall = onDebugFall,
                wearingStatus = wearingStatus,
                onDangerousHrSuggestion = onDangerousHrSuggestion
            )

            // Page 1 → Emergency contacts
            1 -> EmergencyContactsScreen(
                contacts = contacts,
                onCallContact = { contact ->
                    // Perform the call
                    onCallContact(contact)

                    // Return to heart rate screen
                    scope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                }
            )
        }
    }
}
