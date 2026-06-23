package com.childlearning.robot.ui.screens.experiment

import androidx.lifecycle.ViewModel
import com.childlearning.robot.core.audio.TtsPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ExperimentViewModel @Inject constructor(
    val ttsPlayer: TtsPlayer
) : ViewModel()
