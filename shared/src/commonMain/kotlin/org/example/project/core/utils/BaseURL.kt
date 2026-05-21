/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-mobile/blob/master/LICENSE.md
 */
package org.example.project.core.utils

class BaseURL {
    val url: String
        get() = PROTOCOL_HTTPS + API_ENDPOINT + API_PATH

    val defaultBaseUrl: String
        get() = PROTOCOL_HTTPS + API_ENDPOINT

    fun getUrl(endpoint: String): String {
        return endpoint + API_PATH
    }

    companion object {
        const val API_ENDPOINT = ""
//        const val API_PATH = "10.0.2.2:8080/api/v1"
        const val API_PATH = "192.168.29.196:8080/api/v1"
        const val PROTOCOL_HTTPS = "http://"
    }
}
