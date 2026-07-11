package org.example.project.core.window

enum class WindowMode {
    INITIAL, GROWING, MAXIMUM, EXPANSION_BUFFER, SLIDING
}

data class WindowState<T>(
    val limit: Int,
    val anchor: T?,
    val mode: WindowMode
)

/**
 * Pure state machine that determines what portion of the feed should be visible.
 * It manages expanding, buffering (hysteresis), and sliding the window.
 */
class WindowEngine<T>(
    private val config: FeedConfig = FeedConfig()
) {
    private var currentState = WindowState<T>(
        limit = config.initialWindow,
        anchor = null,
        mode = WindowMode.INITIAL
    )

    fun getState(): WindowState<T> = currentState

    fun expand(anchor: T?): WindowState<T> {
        val currentLimit = currentState.limit
        val oldLimit = currentLimit
        val oldAnchor = currentState.anchor
        val oldMode = currentState.mode

        val newState = if (currentLimit < config.maxWindow) {
            val newLimit = (currentLimit + config.step).coerceAtMost(config.maxWindow)
            val mode = if (newLimit == config.maxWindow) WindowMode.MAXIMUM else WindowMode.GROWING
            WindowState(limit = newLimit, mode = mode, anchor = oldAnchor)
        } else if (currentLimit == config.maxWindow) {
            WindowState(limit = config.maxWindow + config.buffer, mode = WindowMode.EXPANSION_BUFFER, anchor = oldAnchor)
        } else {
            WindowState(limit = config.maxWindow, anchor = anchor, mode = WindowMode.SLIDING)
        }

        currentState = newState
        
        println("[WINDOW] Current Mode: $oldMode Limit: $oldLimit Anchor: $oldAnchor")
        println("[WINDOW] Next Mode: ${newState.mode} Limit: ${newState.limit} Anchor: ${newState.anchor}")
        
        return currentState
    }

    fun reset(): WindowState<T> {
        currentState = WindowState(
            limit = config.initialWindow,
            anchor = null,
            mode = WindowMode.INITIAL
        )
        return currentState
    }
}
