package com.webasyst.api

import androidx.annotation.CallSuper
import com.google.gson.reflect.TypeToken
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import java.io.IOException
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
    private val client = config.httpClient
    protected val gson = config.gson
    private val tokenCache = config.tokenCache
    protected val scope = config.scope
    private val installationId = installation.id
    val urlBase = installation.urlBase
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
    }

    protected suspend fun getToken(url: String, authCode: String): AccessToken {
        var response: HttpResponse? = null
        var token: AccessToken? = null
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

            token = response.parse(object : TypeToken<AccessToken>() {})

            if (token.error != null) {
                throw WebasystException(
                    apiModule = this,
                    response = response,
                    token = token,
                    cause = null,
                )
            }
            tokenCache.set(url, joinedScope, token)
            return token
        } catch (e: IOException) {
            throw WebasystException(
                apiModule = this,
                errorCode = WebasystException.ERROR_CONNECTION_FAILED,
                cause = e,
            )
        } catch (e: Throwable) {
            throw WebasystException(
                apiModule = this,
                response = response,
                token = token,
                cause = e,
            )
        }
    }

    /**
     * GET http request wrapper.
     *
     *  Performs basic request configuration (with [configureRequest]), then applies [block].
     * @param urlString request url. Make sure to call urls only within Webasyst installation
     * as it will leak your access token otherwise.
     */
    protected suspend inline fun <reified T> get(urlString: String, crossinline block: HttpRequestBuilder.() -> Unit = {}): Response<T> = apiRequest {
        request {
            method = HttpMethod.Get
            url(urlString)
            apply(block)
        }
    }

    /**
     * POST http request wrapper.
     *
     * Performs basic request configuration (with [configureRequest]), then applies [block].
     * @param urlString request url. Make sure to call urls only within Webasyst installation
     * as it will leak your access token otherwise.
     */
    protected suspend inline fun <reified T> post(urlString: String, crossinline block: HttpRequestBuilder.() -> Unit = {}): Response<T> = apiRequest {
        request {
            contentType(ContentType.Application.Json)
            method = HttpMethod.Post
            url(urlString)
            apply(block)
        }
    }

    protected suspend inline fun <reified T> request(noinline block: HttpRequestBuilder.() -> Unit): T {
        var response: HttpResponse? = null
        try {
            response = performRequest(block)
            return response.parse(object : TypeToken<T>() {})
        } catch (e: Throwable) {
            when {
                e is WebasystException ->
                    throw e
                null != response ->
                    throw WebasystException(this, response, e)
                else ->
                    throw WebasystException(
                        errorCode = WebasystException.ERROR_CONNECTION_FAILED,
                        apiModule = this,
                        cause = e,
                    )
            }
        }
    }

    protected suspend fun performRequest(block: HttpRequestBuilder.() -> Unit): HttpResponse =
        client.request {
            apply(block)
            configureRequest()
        }

    protected suspend fun <T> HttpResponse.parse(typeToken: TypeToken<T>): T {
        if (status.value >= 400) {
            throw WebasystException(
                apiModule = this@ApiModule,
                response = this,
                cause = null,
            )
        }

        val body = this.readText(Charset.forName("UTF8"))
        try {
            return gson.fromJson(body, typeToken.type)
        } catch (e: Throwable) {
            throw WebasystException(
                apiModule = this@ApiModule,
                response = this,
                cause = e,
            )
        }
    }

    companion object {
        const val ACCESS_TOKEN = "access_token"
    }
}
