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
 * ---------------------------------------------------------------------------
 * PAGING PIPELINE STEP 2: WINDOW ENGINE (SLIDING WINDOW ALGORITHM)
 * ---------------------------------------------------------------------------
 * 
 * Purpose:
 * Pure state machine that calculates what portion of the feed should be visible from Room.
 * When users scroll down deeply into the feed (e.g., thousands of items), querying Room 
 * for *all* loaded items would consume too much memory and freeze the UI. 
 * The WindowEngine solves this using a "Sliding Window" algorithm.
 * 
 * Flow:
 * 1. INITIAL: Starts with an initial window limit (e.g. 20 items).
 * 2. GROWING: As user scrolls, the limit expands (e.g. 40, 60, 80) until it hits maxWindow.
 * 3. EXPANSION_BUFFER: Prevents abrupt sliding by letting the window overshoot slightly.
 * 4. SLIDING: Once maxWindow + buffer is reached, it takes an `anchor` (a post ID further down).
 *    Future queries to Room will start from this anchor and fetch a fixed limit. 
 *    This effectively "slides" the top of the feed out of memory, keeping RAM usage perfectly flat!
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
