package com.czh.rediscompose.design.theme

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextInputFocusTrackerTest {
    @Test
    fun `counts focused inputs and never goes negative`() {
        val tracker = TextInputFocusTracker()
        assertFalse(tracker.isTextInputFocused)

        tracker.update(true)
        tracker.update(true)
        assertTrue(tracker.isTextInputFocused)

        tracker.update(false)
        tracker.update(false)
        assertFalse(tracker.isTextInputFocused)

        tracker.update(false)
        assertFalse(tracker.isTextInputFocused)
    }
}
