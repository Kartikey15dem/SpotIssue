package org.example.project.core.database.di

import org.example.project.core.database.IssueSpotDatabase
import org.koin.dsl.module

val DaoModule =
    module {
        includes(platformDatabaseModule)
        single { get<IssueSpotDatabase>().postDao() }
        single { get<IssueSpotDatabase>().likedPostDao() }
        single { get<IssueSpotDatabase>().profileDao() }
        single { get<IssueSpotDatabase>().cacheMetadataDao() }
        single { get<IssueSpotDatabase>().userPostDao() }
        single { get<IssueSpotDatabase>().activeIssuesDao() }
    }
