package com.webasyst.waid

import com.google.gson.Gson
import com.webasyst.api.ApiError
import com.webasyst.api.Response
import com.webasyst.api.WAIDAuthenticator
import com.webasyst.api.WaidException
import com.webasyst.api.apiRequest
import com.webasyst.api.util.useReader
import com.webasyst.auth.WebasystAuthService
import com.webasyst.auth.withFreshAccessToken
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.accept
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.javaio.copyTo
import java.io.File
import java.util.Calendar

class WAIDClient(
    private val authService: WebasystAuthService,
    engine: HttpClientEngine,
    private val waidHost: String,
) : WAIDAuthenticator {
    private val client = HttpClient(engine) {
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }

    private val gson by lazy { Gson() }

    /**
     * POST /id/api/v1/cloud/extend/
     */
    suspend fun cloudExtend(clientId: String, expireDate: Calendar): Response<Unit> = apiRequest {
        val res = doPost<HttpResponse>("$waidHost$CLOUD_EXTEND", CloudExtendRequest(clientId, expireDate))
        if (res.status != HttpStatusCode.NoContent) {
            val error: ApiError = res.content.useReader {
                gson.fromJson(it, ApiError::class.java)
            }
            throw WaidException(error)
        }
    }

    /**
     * POST /id/api/v1/licenses/force
     */
    suspend fun forceLicense(clientId: String, slug: String): Response<Unit> =
        apiRequest {
            val res = doPost<HttpResponse>("$waidHost$FORCE_LICENSE_PATH", ForceLicenseRequest(clientId, slug))
            if (res.status.value >= 400) {
                val error = res.content.useReader {
                    gson.fromJson(it, ApiError::class.java)
                }
                throw WaidException(error)
            }
        }

    suspend fun getInstallationList(): Response<List<Installation>> =
        apiRequest { doGet("$waidHost$INSTALLATION_LIST_PATH") }

    suspend fun getUserInfo(): Response<UserInfo> =
        apiRequest { doGet("$waidHost$USER_LIST_PATH") }

    override suspend fun getInstallationApiAuthCodes(appClientIDs: Set<String>): Response<Map<String, String>> =
        try {
            val r = doPost<Map<String, String>>("$waidHost$CLIENT_LIST_PATH", ClientTokenRequest(appClientIDs))
            Response.success(r)
        } catch (e: Throwable) {
            Response.failure(WaidException(e))
        }

    suspend fun postCloudSignUp(): Response<CloudSignup> = apiRequest {
        authService.withFreshAccessToken { accessToken ->
            client.post("$waidHost$CLOUD_SIGNUP_PATH") {
                headers {
                    accept(ContentType.Application.Json)
                    append("Authorization", "Bearer $accessToken")
                }
            }
        }
    }

    /**
     * POST /id/api/v1/cloud/signup/
     */
    suspend fun postCloudSignUp(build: CloudSignup.Builder.() -> Unit): Response<CloudSignupResponse> = apiRequest {
        authService.withFreshAccessToken { accessToken ->
            client.post("$waidHost$CLOUD_SIGNUP_PATH") {
                headers {
                    accept(ContentType.Application.Json)
                    append("Authorization", "Bearer $accessToken")
                }
                contentType(ContentType.Application.Json)
                body = CloudSignup(build)
            }
        }
    }

    suspend fun downloadUserpic(url: String, file: File): Unit =
        downloadFile(url, file)

    suspend fun signOut(): Response<Unit> = apiRequest {
        authService.withFreshAccessToken { accessToken ->
            client.delete("$waidHost$SIGN_OUT_PATH") {
                headers {
                    append("Authorization", "Bearer $accessToken")
                }
            }
        }
    }

    private suspend inline fun <reified T> doGet(url: String, params: Map<String, String>? = null): T =
        authService.withFreshAccessToken { accessToken ->
            client.get(url) {
                params?.let {
                    it.forEach { (key, value) ->
                        parameter(key, value)
                    }
                }
                headers {
                    accept(ContentType.Application.Json)
                    append("Authorization", "Bearer $accessToken")
                }
            }
        }

    private suspend inline fun <reified T> doPost(url: String, data: Any): T =
        authService.withFreshAccessToken { accessToken ->
            client.post(url) {
                headers {
                    accept(ContentType.Application.Json)
                    append("Authorization", "Bearer $accessToken")
                }
                contentType(ContentType.Application.Json)
                body = data
            }
        }

    private suspend inline fun downloadFile(url: String, file: File) {
        val response = client.request<HttpResponse> {
            url(url)
            method = HttpMethod.Get
        }

        if (response.status.isSuccess()) {
            if (file.exists()) {
                file.delete()
            }
            file.outputStream().use { fo ->
                response.content.copyTo(fo)
            }
        }

    }

    companion object {
        private const val SIGN_OUT_PATH = "/id/api/v1/delete/"
        private const val CLOUD_EXTEND = "/id/api/v1/cloud/extend/"
        private const val CLOUD_SIGNUP_PATH = "/id/api/v1/cloud/signup/"
        private const val FORCE_LICENSE_PATH = "/id/api/v1/licenses/force"
        private const val INSTALLATION_LIST_PATH = "/id/api/v1/installations/"
        private const val USER_LIST_PATH = "/id/api/v1/profile/"
        private const val CLIENT_LIST_PATH = "/id/api/v1/auth/client/"
    }
}
