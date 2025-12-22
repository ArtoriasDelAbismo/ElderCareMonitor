package com.example.eldercaremonitor.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagerScreen(
    heartRate: Int?,
    contacts: List<EmergencyContact>,
    onPanic: () -> Unit,
    wearingStatus: String,
    onCallContact: (EmergencyContact) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        when (page) {
            0 -> HeartRateScreen(
                hr = heartRate,
                onPanic = onPanic,
                onDebugFall = {},
                wearingStatus = wearingStatus
            )

            1 -> EmergencyContactsScreen(
                contacts = contacts,
                onCallContact = onCallContact
            )
        }
    }


}