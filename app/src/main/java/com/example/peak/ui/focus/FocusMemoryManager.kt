package com.example.peak.ui.focus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateMapOf

/**
 * Manages focus memory for rows in a TV list.
 * Remembers which item was last focused in a specific row to ensure
 * smooth vertical navigation without resetting to the first item.
 */
class FocusMemoryManager {
    // Key: rowId (unique row identifier), Value: itemId (last focused item in that row)
    private val memory = mutableStateMapOf<String, String>()

    /**
     * Saves the last focused item ID for a given row.
     */
    fun saveFocus(rowId: String, itemId: String) {
        memory[rowId] = itemId
    }

    /**
     * Retrieves the last focused item ID for a given row.
     */
    fun getRememberedId(rowId: String): String? {
        return memory[rowId]
    }
}

/**
 * Creates and remembers a FocusMemoryManager instance.
 */
@Composable
fun rememberFocusMemoryManager(): FocusMemoryManager {
    return remember { FocusMemoryManager() }
}
