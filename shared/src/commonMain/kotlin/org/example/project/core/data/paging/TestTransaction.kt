package org.example.project.core.data.paging

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import org.example.project.core.database.IssueSpotDatabase

suspend fun doTransaction(db: IssueSpotDatabase) {
    (db as Any as RoomDatabase).withTransaction {
        // do nothing
    }
}
