package com.michaelzhou.clockapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class ClockViewModel : ViewModel() {
    private val _time = MutableStateFlow(LocalDateTime.now())
    val time: StateFlow<LocalDateTime> = _time

    init {
        viewModelScope.launch {
            while (true) {
                _time.value = LocalDateTime.now()
                delay(1000)
            }
        }
    }
}
