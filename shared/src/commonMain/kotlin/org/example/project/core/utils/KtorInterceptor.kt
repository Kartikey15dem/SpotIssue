package org.example.project.core.utils

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponsePipeline
import io.ktor.http.HttpStatusCode
import io.ktor.util.AttributeKey

class KtorInterceptor(
    private val getToken: () -> String?,
) {
    companion object Plugin : HttpClientPlugin<Config, KtorInterceptor> {

        private const val HEADER_AUTH = "Authorization"
        private const val HEADER_CONTENT_TYPE = "Content-Type"
        private const val HEADER_ACCEPT = "Accept"

        override val key: AttributeKey<KtorInterceptor> = AttributeKey("KtorInterceptor")

        override fun install(plugin: KtorInterceptor, scope: HttpClient) {

            scope.requestPipeline.intercept(HttpRequestPipeline.State) {
                context.header(HEADER_CONTENT_TYPE, "application/json")
                context.header(HEADER_ACCEPT, "application/json")

                plugin.getToken()?.let { token ->
                    if (token.isNotEmpty()) {
                        context.headers[HEADER_AUTH] = "Bearer $token"
                    }
                }
            }

            // 2. Intercept incoming responses
//            scope.responsePipeline.intercept(HttpResponsePipeline.After) {
//                // This is where you will handle expired JWT tokens later!
//                if (context.response.status == HttpStatusCode.Unauthorized) {
//
//                }
//                proceed()
//            }
        }

        override fun prepare(block: Config.() -> Unit): KtorInterceptor {
            val config = Config().apply(block)
            return KtorInterceptor(
                getToken = config.getToken,
            )
        }
    }
}

class Config {
    lateinit var getToken: () -> String?
}