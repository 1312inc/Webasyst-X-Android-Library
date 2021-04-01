package com.webasyst.api

import androidx.annotation.CallSuper
import com.google.gson.reflect.TypeToken
import io.ktor.client.HttpClient
import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.ContentType
import io.ktor.util.toByteArray
import java.nio.charset.Charset

/**
 * Base class for all application-specific API modules
 */
abstract class ApiModule(
    config: ApiClientConfiguration,
    installation: Installation,
    private val waidAuthenticator: WAIDAuthenticator,
) {
    protected val client = config.httpClient
    protected val gson = config.gson
    private val tokenCache = config.tokenCache
    protected val scope = config.scope
    private val installationId = installation.id
    protected val urlBase = installation.urlBase
    private val joinedScope = scope.joinToString(separator = ",")

    /**
     * Configures http request - sets appropriate headers and adds "access_token" query parameter
     */
    @CallSuper
    open suspend fun HttpRequestBuilder.configureRequest() {
        val accessToken = getToken()
        parameter(ACCESS_TOKEN, accessToken.token)
        accept(ContentType.Application.Json)
    }

    /**
     * Returns application [AccessToken].
     * First tries to retrieve it from cache.
     * If it fails requests new token and stores it in cache to be reused in further requests.
     */
    suspend fun getToken(): AccessToken {
        try {
            val cached = tokenCache.get(url = urlBase, scope = joinedScope)
            if (null != cached) return cached

            val authCodesResponse = waidAuthenticator.getInstallationApiAuthCodes(setOf(installationId))
            if (authCodesResponse.isFailure()) throw authCodesResponse.getFailureCause()

            val authCodes = authCodesResponse.getSuccess()
            val authCode = authCodes[installationId]
                ?: throw RuntimeException("Failed to obtain authorization code")

            return getToken(urlBase, authCode)
        } catch (e: Throwable) {
            throw TokenException(e)
        }
    }

    protected suspend fun getToken(url: String, authCode: String): AccessToken {
        try {
            val response = client.post<String>("$url/api.php/token-headless") {
                headers {
                    accept(ContentType.Application.Json)
                }
                body = MultiPartFormDataContent(formData {
                    append("code", authCode)
                    append("scope", joinedScope)
                    append("client_id", "com.webasyst.x")
                })
            }

            val token = gson.fromJson(response, AccessToken::class.java)
            if (token.error != null) {
                throw TokenError(token)
            }
            tokenCache.set(url, joinedScope, token)
            return token
        } catch (e: Throwable) {
            throw TokenException(e)
        }
    }

    /**
     * GET http request wrapper.
     *
     *  Performs basic request configuration (with [configureRequest]), then applies [block].
     * @param urlString request url. Make sure to call urls only within Webasyst installation
     * as it will leak your access token otherwise.
     */
    protected suspend inline fun <reified T> HttpClient.doGet(urlString: String, block: HttpRequestBuilder.() -> Unit = {}) = apiRequest {
        try {
            get<HttpResponse>(urlString) {
                configureRequest()
                apply(block)
            }.parse(object : TypeToken<T>() {})
        } catch (e: ClientRequestException) {
            val body = e.response.readText(Charset.forName("UTF-8"))
            throw ApiError(gson.fromJson(body, ApiError.ApiCallResponse::class.java), e)
        }
    }

    /**
     * POST http request wrapper.
     *
     * Performs basic request configuration (with [configureRequest]), then applies [block].
     * @param urlString request url. Make sure to call urls only within Webasyst installation
     * as it will leak your access token otherwise.
     */
    protected suspend inline fun <reified T> HttpClient.doPost(urlString: String, block: HttpRequestBuilder.() -> Unit = {}) = apiRequest {
        post<HttpResponse>(urlString) {
            configureRequest()
            apply(block)
        }.parse(object : TypeToken<T>() {})
    }

    protected suspend fun <T> HttpResponse.parse(typeToken: TypeToken<T>): T {
        val body = content.toByteArray().toString(Charset.forName("UTF8"))
        try {
            return gson.fromJson(body, typeToken.type)
        } catch (e: Throwable) {
            throw ApiError(gson.fromJson(body, ApiError.ApiCallResponse::class.java))
        }
    }

    companion object {
        const val ACCESS_TOKEN = "access_token"
    }
}
