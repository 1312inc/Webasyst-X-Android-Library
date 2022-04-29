package com.webasyst.waid

import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.webasyst.api.ApiModuleInfo
import com.webasyst.api.Response
import com.webasyst.api.WAIDAuthenticator
import com.webasyst.api.WebasystException
import com.webasyst.api.apiRequest
import com.webasyst.api.util.GsonInstance
import com.webasyst.auth.WebasystAuthService
import com.webasyst.auth.withFreshAccessToken
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.accept
import io.ktor.client.request.delete
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.javaio.copyTo
import net.openid.appauth.TokenRequest
import net.openid.appauth.TokenResponse
import java.io.File
import java.nio.charset.Charset
import java.util.Calendar

class WAIDClient(
    public val authService: WebasystAuthService,
    engine: HttpClientEngine,
    private val waidHost: String,
) : WAIDAuthenticator, ApiModuleInfo {
    override val appName: String = "WAID"
    override val urlBase: String get() = waidHost

    private val client = HttpClient(engine) {
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }

    private val gson by GsonInstance

    /**
     * POST /id/api/v1/cloud/extend/
     */
    suspend fun cloudExtend(clientId: String, expireDate: Calendar): Response<Unit> = apiRequest {
        val res = doPost<HttpResponse>("$waidHost$CLOUD_EXTEND", CloudExtendRequest(clientId, expireDate))
        if (res.status != HttpStatusCode.NoContent) {
            throw WebasystException {
                withApiModule(this@WAIDClient)
                withHttpResponse(res)
            }
        }
    }

    /**
     * POST /id/api/v1/licenses/force
     */
    suspend fun forceLicense(clientId: String, slug: String): Response<Unit> =
        apiRequest {
            val res = doPost<HttpResponse>("$waidHost$FORCE_LICENSE_PATH", ForceLicenseRequest(clientId, slug))
            if (res.status.value >= 400) {
                throw WebasystException {
                    withApiModule(this@WAIDClient)
                    withHttpResponse(res)
                }
            }
        }

    suspend fun getInstallationList(): Response<List<Installation>> =
        apiRequest { doGet("$waidHost$INSTALLATION_LIST_PATH") }

    suspend fun getUserInfo(): Response<UserInfo> =
        apiRequest { doGet("$waidHost$USER_LIST_PATH") }

    override suspend fun getInstallationApiAuthCodes(appClientIDs: Set<String>): Response<Map<String, String>> {
        var res: HttpResponse? = null
        return try {
            res = doPost<HttpResponse>("$waidHost$CLIENT_LIST_PATH", ClientTokenRequest(appClientIDs))
            val r = gson.fromJson<Map<String, String>>(
                res.readText(Charset.forName("Utf-8")),
                (object : TypeToken<Map<String, String>>() {}).type
            )
            Response.success(r)
        } catch (e: Throwable) {
            Response.failure(
                WebasystException {
                    withApiModule(this@WAIDClient)
                    if (null != res) {
                        withHttpResponse(res)
                    }
                }
            )
        }
    }

    suspend fun postCloudSignUp(): Response<CloudSignupResponse> = apiRequest {
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

    suspend fun postAuthCode(clientId: String, scope: String, locale: String, email: String?, phone: String?): HeadlessCodeRequestResult {
        if (email != null && phone != null) {
            throw IllegalArgumentException("Either email or phone must be set. Not both.")
        }
        if (email == null && phone == null) {
            throw IllegalArgumentException("Either email or phone must be set")
        }
        val codeChallenge = CodeChallenge()
        val response = apiRequest {
            client.post<String>("$waidHost$HEADLESS_CODE_PATH") {
                body = FormDataContent(Parameters.build {
                    append("client_id", clientId)
                    append("device_id", authService.configuration.deviceId)
                    append("code_challenge", codeChallenge.encoded)
                    append("code_challenge_method", codeChallenge.challengeMethod)
                    append("scope", scope)
                    append("locale", locale)
                    if (null != email) {
                        append("email", email)
                    }
                    if (null != phone) {
                        append("phone", phone)
                    }
                })
            }
        }
        if (response.isSuccess()) {
            val postAuthCodeResponse = gson.fromJson(response.getSuccess(), PostAuthCodeResponse::class.java)
            return HeadlessCodeRequestResult(
                nextRequestAllowedAt = postAuthCodeResponse.nextRequestAllowedAt,
                codeChallenge = codeChallenge,
            )
        } else {
            throw response.getFailureCause()
        }
    }
    private class PostAuthCodeResponse(
        @SerializedName("next_request_allowed_at")
        val nextRequestAllowedAt: Long,
    )

    suspend fun postHeadlessToken(clientId: String, codeVerifier: String, code: String): HeadlessTokenResponse {
        val response = apiRequest {
            client.post<HeadlessTokenResponse>("$waidHost$HEADLESS_TOKEN_PATH") {
                body = FormDataContent(Parameters.build {
                    append("client_id", clientId)
                    append("device_id", authService.configuration.deviceId)
                    append("code_verifier", codeVerifier)
                    append("code", code)
                })
            }
        }

        if (response.isSuccess()) {
            return response.getSuccess()
        } else {
            throw response.getFailureCause()
        }
    }
    fun tokenResponseFromHeadlessRequest(res: HeadlessTokenResponse) =
        TokenResponse
            .Builder(
                TokenRequest
                    .Builder(authService.authServiceConfiguration, authService.configuration.clientId)
                    .setGrantType("true_sign_in")
                    .build()
            )
            .setAccessToken(res.accessToken)
            .setAccessTokenExpiresIn(res.expiresIn)
            .setRefreshToken(res.refreshToken)
            .setTokenType(res.tokenType)
            .build()

    suspend fun downloadUserpic(url: String, file: File): Unit =
        downloadFile(url, file)

    suspend fun signOut(): Response<Unit> = apiRequest {
        authService.withFreshAccessToken { accessToken ->
            signOut(accessToken)
        }
    }

    suspend fun signOut(accessToken: String): Response<Unit> =
        client.delete("$waidHost$SIGN_OUT_PATH") {
            headers {
                append("Authorization", "Bearer $accessToken")
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
        private const val HEADLESS_CODE_PATH = "/id/oauth2/auth/headless/code/"
        private const val HEADLESS_TOKEN_PATH = "/id/oauth2/auth/headless/token/"

        suspend fun signOut(httpClient: HttpClient, waidHost: String, accessToken: String): Response<String> = apiRequest {
            httpClient.delete("$waidHost$SIGN_OUT_PATH") {
                headers {
                    append("Authorization", "Bearer $accessToken")
                }
            }
        }
    }
}
