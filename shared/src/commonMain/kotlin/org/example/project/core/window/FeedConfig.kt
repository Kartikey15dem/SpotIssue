package org.example.project.core.window

data class FeedConfig(
    val initialWindow: Int = 20,
    val step: Int = 20,
    val maxWindow: Int = 120,
    val buffer: Int = 20
) {
    companion object {
        const val LOAD_MORE_THRESHOLD = 5
    }
}
