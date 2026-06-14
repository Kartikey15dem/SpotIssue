package org.example.project.core.utils

import org.example.project.feature.profile.viewmodel.CounterViewModel
import org.koin.mp.KoinPlatform.getKoin

object KoinHelper {

    fun getCounterViewModel(): CounterViewModel {
        return getKoin().get()
    }
}