package org.example.project.core.presentationcache

/**
 * ---------------------------------------------------------------------------
 * PAGING PIPELINE STEP 4: PRESENTATION CACHE (UI STABILIZATION)
 * ---------------------------------------------------------------------------
 * 
 * Purpose:
 * Transforms continuous Room emissions (from Step 3) into stable UI updates.
 * In Jetpack Compose and SwiftUI, replacing the entire list reference forces
 * the UI to discard its scroll state and aggressively recompose.
 * 
 * How it works:
 * 1. Maintains a long-lived `MutableList` internally.
 * 2. When Room emits a new snapshot of the database, it compares it against the internal list.
 * 3. It performs a lightweight O(N) diff and updates the internal list IN PLACE.
 * 4. This means items that haven't changed retain their exact memory identity,
 *    and Compose/SwiftUI can perform smooth inserts/deletes instead of flash-reloading.
 * 
 * @param T The type of item (e.g., Post)
 * @param K The unique identifier type (e.g., String ID)
 */
class PresentationCache<T, K>(
    private val idSelector: (T) -> K
) {
    private val _items = mutableListOf<T>()
    
    // Read-only view for the UI. The list reference stays exactly the same.
    val items: List<T> get() = _items

    fun clear() {
        _items.clear()
    }

    fun update(roomPosts: List<T>) {
        val oldSize = _items.size
        
        if (roomPosts.isEmpty()) {
            _items.clear()
            return
        }

        // 1. Clean duplicates from Room list
        val cleanRoomList = mutableListOf<T>()
        val roomIds = mutableSetOf<K>()
        for (item in roomPosts) {
            val id = idSelector(item)
            if (roomIds.add(id)) {
                cleanRoomList.add(item)
            } else {
            }
        }

        var removedCount = 0
        var updatedCount = 0
        var insertedCount = 0
        var retainedCount = 0

        // 2. Remove missing items (walk backwards)
        for (i in _items.indices.reversed()) {
            val id = idSelector(_items[i])
            if (!roomIds.contains(id)) {
                _items.removeAt(i)
                removedCount++
            }
        }

        // 3 & 4. Update existing items and insert new items in place
        for (i in cleanRoomList.indices) {
            val roomItem = cleanRoomList[i]
            val roomId = idSelector(roomItem)

            if (i < _items.size) {
                val currentItem = _items[i]
                val currentId = idSelector(currentItem)

                if (currentId == roomId) {
                    if (currentItem != roomItem) {
                        _items[i] = roomItem
                        updatedCount++
                    } else {
                        retainedCount++
                    }
                } else {
                    _items.add(i, roomItem)
                    insertedCount++
                }
            } else {
                _items.add(roomItem)
                insertedCount++
            }
        }
        
        while (_items.size > cleanRoomList.size) {
            _items.removeLast()
        }

    }
}
