package com.shp.evernote

import com.shp.evernote.ui.home.HomeViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Smoke test so CI's unit-test step has something real to run. */
class HomeViewModelTest {
    @Test
    fun `initial state is empty and carries the version name`() {
        val state = HomeViewModel().uiState.value
        assertEquals(0, state.noteCount)
        assertTrue(state.versionName.isNotBlank())
    }
}
