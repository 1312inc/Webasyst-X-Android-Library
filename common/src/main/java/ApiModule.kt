package com.webasyst.api

import androidx.annotation.CallSuper
import com.google.gson.reflect.TypeToken
import io.ktor.client.HttpClient
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
import java.nio.charset.Charset

/**
 * Base class for all application-specific API modules
 */
abstract class ApiModule(
    config: ApiClientConfiguration,
    installation: Installation,
    private val waidAuthenticator: WAIDAuthenticator,
) {
    abstract val appName: String
    protected val client = config.httpClient
    protected val gson = config.gson
    private val tokenCache = config.tokenCache
    protected val scope = config.scope
    private val installationId = installation.id
    protected val urlBase = installation.urlBase
    private val joinedScope = scope.joinToString(separator = ",")
    private val clientId = config.clientId

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

            val cachedAuthCode = tokenCache.getAuthCode(installationId)
            val authCode = if (null != cachedAuthCode) {
                cachedAuthCode
            } else {
                val authCodesResponse = waidAuthenticator.getInstallationApiAuthCodes(setOf(installationId))
                if (authCodesResponse.isFailure()) throw authCodesResponse.getFailureCause()

                val authCodes = authCodesResponse.getSuccess()
                val code = authCodes[installationId]
                if (null != code) {
                    tokenCache.setAuthCode(installationId, code)
                }
                code ?: throw RuntimeException("Failed to obtain authorization code")
            }

            return getToken(urlBase, authCode)
        } catch (e: Throwable) {
            if (e is WebasystException) {
                throw e
            } else {
                throw WebasystException(
                    webasystCode = WebasystException.UNRECOGNIZED_ERROR,
                    webasystMessage = "A error occurred while obtaining access token",
                    webasystApp = appName,
                    webasystHost = urlBase,
                    cause = e,
                )
            }
        }
    }

    protected suspend fun getToken(url: String, authCode: String): AccessToken {
        var response: HttpResponse? = null
        try {
            response = client.post<HttpResponse>("$url/api.php/token-headless") {
                headers {
                    accept(ContentType.Application.Json)
                }
                body = MultiPartFormDataContent(formData {
                    append("code", authCode)
                    append("scope", joinedScope)
                    append("client_id", clientId)
                })
            }

            val token = response.parse(object : TypeToken<AccessToken>() {})

            if (token.error != null) {
                throw WebasystException(
                    webasystCode = token.error,
                    webasystMessage = token.errorDescription ?: "",
                    webasystApp = appName,
                    webasystHost = urlBase,
                )
            }
            tokenCache.set(url, joinedScope, token)
            return token
        } catch (e: Throwable) {
            if (null != response) {
                throw WebasystException(
                    response = response,
                    cause = e,
                    webasystApp = appName,
                    webasystHost = urlBase,
                )
            } else {
                throw WebasystException(
                    webasystCode = WebasystException.UNRECOGNIZED_ERROR,
                    webasystMessage = "A error occurred while obtaining access token",
                    webasystApp = appName,
                    webasystHost = urlBase,
                    cause = e,
                )
            }
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
        var response: HttpResponse? = null
        try {
            response = get<HttpResponse>(urlString) {
                configureRequest()
                apply(block)
            }
            response.parse(object : TypeToken<T>() {})
        } catch (e: Throwable) {
            when {
                e is WebasystException ->
                    throw e
                null != response ->
                    throw WebasystException(response, e, appName, urlBase)
                else ->
                    throw WebasystException(
                        webasystCode = WebasystException.ERROR_CONNECTION_FAILED,
                        webasystMessage = "Failed to connect to $urlBase",
                        webasystApp = appName,
                        webasystHost = urlBase,
                        cause = e
                    )
            }
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
        var response: HttpResponse? = null
        try {
            response = post<HttpResponse>(urlString) {
                configureRequest()
                apply(block)
            }
            response.parse(object : TypeToken<T>() {})
        } catch (e: Throwable) {
            when {
                e is WebasystException ->
                    throw e
                null != response ->
                    throw WebasystException(response, e, appName, urlBase)
                else ->
                    throw WebasystException(
                        webasystCode = WebasystException.ERROR_CONNECTION_FAILED,
                        webasystMessage = "Failed to connect to $urlBase",
                        webasystApp = appName,
                        webasystHost = urlBase,
                        cause = e
                    )
            }
        }
    }

    protected suspend fun <T> HttpResponse.parse(typeToken: TypeToken<T>): T {
        if (status.value >= 400) {
            throw WebasystException(this, null, appName, urlBase)
        }

        val body = this.readText(Charset.forName("UTF8"))
        try {
            return gson.fromJson(body, typeToken.type)
        } catch (e: Throwable) {
            throw WebasystException(this, e, appName, urlBase,)
        }
    }

    companion object {
        const val ACCESS_TOKEN = "access_token"
    }
}
