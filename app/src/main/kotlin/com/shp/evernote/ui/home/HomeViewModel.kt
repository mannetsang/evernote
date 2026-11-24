package com.shp.evernote.ui.home

import androidx.lifecycle.ViewModel
import com.shp.evernote.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class HomeUiState(
    val noteCount: Int = 0,
    val versionName: String = BuildConfig.VERSION_NAME,
)

/**
 * Placeholder for the note list. Exists so the skeleton proves the
 * Hilt + ViewModel + Compose wiring end to end; the real list arrives with
 * the data layer.
 */
@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState
}
