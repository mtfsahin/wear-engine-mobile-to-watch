package com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.domain.models

import androidx.compose.runtime.Immutable
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.data.models.DeviceState
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.data.models.MessageState
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.data.models.FileState
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.data.models.HealthKitState

@Immutable
data class UiState(
    val deviceState: DeviceState = DeviceState(),
    val messageState: MessageState = MessageState(),
    val fileState: FileState = FileState(),
    val healthKitState: HealthKitState = HealthKitState(),
    val receivedMessages: List<String> = emptyList(),
    val logMessages: List<String> = emptyList()
)