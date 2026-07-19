/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
package org.example.project.core.network

import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import org.example.project.core.network.services.createAuthenticationService
import org.example.project.core.network.services.createHomeService
import org.example.project.core.network.services.createPostService
import org.example.project.core.network.services.createProfileService
import kotlin.getValue

class KtorfitClient(
    ktorfit: Ktorfit,
) {
    internal val authenticationApi by lazy { ktorfit.createAuthenticationService() }

    internal val profileApi by lazy { ktorfit.createProfileService() }

    internal val homeApi by lazy { ktorfit.createHomeService() }

    internal val postApi by lazy { ktorfit.createPostService() }

    class Builder internal constructor() {
        private lateinit var baseURL: String
        private lateinit var httpClient: HttpClient

        fun baseURL(baseURL: String): Builder {
            this.baseURL = baseURL
            return this
        }

        fun httpClient(ktorHttpClient: HttpClient): Builder {
            this.httpClient = ktorHttpClient
            return this
        }

        fun build(): KtorfitClient {
            val ktorfitBuilder =
                Ktorfit
                    .Builder()
                    .httpClient(httpClient)
                    .baseUrl(baseURL)
                    .build()
            return KtorfitClient(ktorfitBuilder)
        }
    }

    companion object {
        fun builder(): Builder = Builder()
    }
}
