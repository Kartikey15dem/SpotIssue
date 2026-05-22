package org.example.project.core.network.di

import org.koin.core.module.Module

/**
 * Platform-provided network monitor binding.
 *
 * Keep the repository logic in commonMain by providing platform-specific monitoring via expect/actual.
 */
expect val platformNetworkModule: Module

