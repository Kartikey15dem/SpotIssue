package org.example.project.core.presentationcache

/**
 * Transforms Room emissions into stable UI updates.
 * Preserves the same MutableList instance to maintain stable identity for Compose.
 * Calculates an O(N) diff and performs surgical inserts/removes.
 */
class PresentationCache<T, K>(
    private val idSelector: (T) -> K
) {
    private val _items = mutableListOf<T>()
    
    // Read-only view for Compose
    val items: List<T> get() = _items

    fun clear() {
        _items.clear()
    }

    fun update(roomPosts: List<T>) {
        val oldSize = _items.size
        
        if (roomPosts.isEmpty()) {
            _items.clear()
            println("[PRESENTATION] Old Size = $oldSize Room Size = 0 Inserted = 0 Updated = 0 Removed = $oldSize Retained = 0 Current Size = 0")
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
                println("[PRESENTATION] Error: Duplicate ID found in Room emission: $id")
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

        println("[PRESENTATION] Old Size = $oldSize Room Size = ${roomPosts.size} Inserted = $insertedCount Updated = $updatedCount Removed = $removedCount Retained = $retainedCount Current Size = ${_items.size}")
    }
}
