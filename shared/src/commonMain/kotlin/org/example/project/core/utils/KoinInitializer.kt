package org.example.project.core.utils

import org.example.project.core.config.RazorpayConfig
import org.example.project.core.di.appModules
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

fun initializeKoin(
    additionalModules: List<Module> = emptyList(),
    config: KoinAppDeclaration? = null
) {
    startKoin {
        config?.invoke(this)

        properties(
            mapOf(
                "razorpay.key.id" to RazorpayConfig.KEY_ID,
                "razorpay.key.secret" to RazorpayConfig.KEY_SECRET
            )
        )

        modules(appModules + additionalModules)
    }
}