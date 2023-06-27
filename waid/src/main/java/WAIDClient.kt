package com.webasyst.waid

import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.webasyst.api.ApiModuleInfo
import com.webasyst.api.Response
import com.webasyst.api.WAIDAuthenticator
import com.webasyst.api.WebasystException
import com.webasyst.api.apiRequest
import com.webasyst.api.util.GsonInstance
import com.webasyst.auth.AccessTokenTask
import com.webasyst.auth.WebasystAuthInterface
import com.webasyst.auth.WebasystAuthService
import com.webasyst.auth.withFreshAccessToken
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import io.ktor.serialization.gson.gson
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
        install(ContentNegotiation) {
            gson()
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
                res.bodyAsText(Charset.forName("Utf-8")),
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
            }.body()
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
                setBody(CloudSignup(build))
            }.body()
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
            client.submitForm(
                url = "$waidHost$HEADLESS_CODE_PATH",
                formParameters = parameters {
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
                }
            ).body() as String
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
            client.submitForm(
                url = "$waidHost$HEADLESS_TOKEN_PATH",
                formParameters = parameters {
                    append("client_id", clientId)
                    append("device_id", authService.configuration.deviceId)
                    append("code_verifier", codeVerifier)
                    append("code", code)
                }
            ).body() as HeadlessTokenResponse
        }

        if (response.isSuccess()) {
            return response.getSuccess()
        } else {
            throw response.getFailureCause()
        }
    }

    suspend fun postQrToken(clientId: String, scope: String, code: String): HeadlessTokenResponse =
        client.submitForm(
            url = "$waidHost$QR_TOKEN_PATH",
            formParameters = parameters {
                append("client_id", clientId)
                append("device_id", authService.configuration.deviceId)
                append("scope", scope)
                append("code", code)
            }
        ).body() as HeadlessTokenResponse

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

    suspend fun signOut(accessToken: String): Response<Unit> = apiRequest {
        client.delete("$waidHost$SIGN_OUT_PATH") {
            headers {
                append("Authorization", "Bearer $accessToken")
            }
        }
        Unit
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
            }.body()
        }

    private suspend inline fun <reified T> doPost(url: String, data: Any): T =
        authService.withFreshAccessToken { accessToken ->
            client.post(url) {
                headers {
                    accept(ContentType.Application.Json)
                    append("Authorization", "Bearer $accessToken")
                }
                contentType(ContentType.Application.Json)
                setBody(data)
            }.body()
        }

    private suspend inline fun downloadFile(url: String, file: File) {
        val response = client.request {
            url(url)
            method = HttpMethod.Get
        }

        if (response.status.isSuccess()) {
            if (file.exists()) {
                file.delete()
            }
            file.outputStream().use { fo ->
                response.bodyAsChannel().copyTo(fo)
            }
        }

    }


    suspend fun updateUserInfo(userInfo: UpdateUserInfo): Response<UserInfo> {
        return apiRequest {
            doPatch("$waidHost$USER_LIST_PATH") {
                contentType(ContentType.Application.Json)
                setBody(userInfo)
            }
        }
    }

    suspend fun updateUserpic(userpic: ByteArray): Response<UserpicUploadResponse> {
        return apiRequest {
            doPost("$waidHost$USERPIC_PATH") {
                contentType(ContentType.Image.Any)
                setBody(userpic)
            }
        }
    }

    suspend fun deleteUserpic(): Response<String> {
        return apiRequest {
            doDelete("$waidHost$USERPIC_PATH")
        }
    }

    suspend fun requestMergeCode(): Response<MergeCodeResponse> = apiRequest {
        doGets("$waidHost$MERGE_CODE_PATH")
    }

    private suspend inline fun <reified T> doGets(url: String): T {
        var response: HttpResponse? = null
        return try {
            authService.withFreshAccessToken { accessToken ->
                response = client.get(url) {
                    headers {
                        accept(ContentType.Application.Json)
                        append("Authorization", "Bearer $accessToken")
                    }
                }
                response!!.body()
            }
        } catch (e: Throwable) {
            throw WebasystException {
                withApiModule(this@WAIDClient)
                if (null != response) {
                    withHttpResponse(response!!)
                } else if (e is ClientRequestException) {
                    withHttpResponse(e.response)
                }
                withCause(e)
            }
        }
    }

    private suspend inline fun <reified T> doPatch(url: String, crossinline block: HttpRequestBuilder.() -> Unit): T {
        var response: HttpResponse? = null
        return try {
            authService.withFreshAccessToken { accessToken ->
                response = client.patch(url) {
                    headers {
                        accept(ContentType.Application.Json)
                        append("Authorization", "Bearer $accessToken")
                    }
                    apply(block)
                }
                response!!.body()
            }
        } catch (e: Throwable) {
            throw WebasystException {
                withApiModule(this@WAIDClient)
                if (null != response) {
                    withHttpResponse(response!!)
                } else if (e is ClientRequestException) {
                    withHttpResponse(e.response)
                }
                withCause(e)
            }
        }
    }

    private suspend inline fun <reified T> doPost(url: String, crossinline block: HttpRequestBuilder.() -> Unit): T {
        var response: HttpResponse? = null
        return try {
            authService.withFreshAccessToken { accessToken ->
                response = client.post(url) {
                    headers {
                        accept(ContentType.Application.Json)
                        append("Authorization", "Bearer $accessToken")
                    }
                    apply(block)
                }
                response!!.body()
            }
        } catch (e: Throwable) {
            throw WebasystException {
                withApiModule(this@WAIDClient)
                if (null != response) {
                    withHttpResponse(response!!)
                } else if (e is ClientRequestException) {
                    withHttpResponse(e.response)
                }
                withCause(e)
            }
        }
    }

    private suspend inline fun <reified T> doDelete(url: String): T =
        authService.withFreshAccessToken { accessToken ->
            client.delete(url) {
                headers {
                    append("Authorization", "Bearer $accessToken")
                }
            }.body()
        }

    suspend fun cloudRename(request: CloudRenameRequest): Response<String> = apiRequest {
        doPost("$waidHost$CLOUD_RENAME_PATH") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun connectInstallation(code: String, accessToken: String?): Response<Installation> =
        apiRequest {
            var response: HttpResponse? = null
            try {
                if (accessToken != null){
                    response = client.post("$waidHost$INSTALLATION_CONNECT") {
                        headers {
                            accept(ContentType.Application.Json)
                            append("Authorization", "Bearer $accessToken")
                        }
                        formData {
                            parameter("code", code)
                        }
                    }
                    response.body()
                } else {
                    response = authService.withFreshAccessToken { accessToken ->
                        client.post("$waidHost$INSTALLATION_CONNECT") {
                            headers {
                                accept(ContentType.Application.Json)
                                append("Authorization", "Bearer $accessToken")
                            }
                            formData {
                                parameter("code", code)
                            }
                        }
                    }
                    response.body()
                    /*doPost("$waidHost$INSTALLATION_CONNECT"){
                        formData {
                            parameter("code", code)
                        }
                    }*/
                }
            } catch (e: Throwable) {
                throw WebasystException {
                    withApiModule(this@WAIDClient)
                    if (null != response) {
                        withHttpResponse(response!!)
                    } else if (e is ClientRequestException) {
                        withHttpResponse(e.response)
                    }
                    withCause(e)
                }
            }
        }

    private suspend fun <T> WebasystAuthInterface.withFreshAccessToken(task: suspend (token: String?, exception: Throwable?) -> T): T =
        withFreshAccessToken(object : AccessTokenTask<T> {
            override suspend fun apply(accessToken: String?, exception: Throwable?): T =
                task(accessToken, exception)
        })

    companion object {
        private const val TAG = "com.webasyst.waid"

        private const val SIGN_OUT_PATH = "/id/api/v1/delete/"
        private const val CLOUD_EXTEND = "/id/api/v1/cloud/extend/"
        private const val CLOUD_SIGNUP_PATH = "/id/api/v1/cloud/signup/"
        private const val FORCE_LICENSE_PATH = "/id/api/v1/licenses/force"
        private const val INSTALLATION_LIST_PATH = "/id/api/v1/installations/"
        private const val USER_LIST_PATH = "/id/api/v1/profile/"
        private const val CLIENT_LIST_PATH = "/id/api/v1/auth/client/"
        private const val HEADLESS_CODE_PATH = "/id/oauth2/auth/headless/code/"
        private const val HEADLESS_TOKEN_PATH = "/id/oauth2/auth/headless/token/"

        private const val CLOUD_RENAME_PATH = "/id/api/v1/cloud/rename/"
        private const val USERPIC_PATH = "/id/api/v1/profile/userpic/"
        private const val MERGE_CODE_PATH = "/id/api/v1/profile/mergecode/"
        private const val INSTALLATION_CONNECT = "/id/api/v1/installation/connect/"
        private const val QR_TOKEN_PATH = "/id/oauth2/auth/qr/token/"

        suspend fun signOut(httpClient: HttpClient, waidHost: String, accessToken: String): Response<String> = apiRequest {
            httpClient.delete("$waidHost$SIGN_OUT_PATH") {
                headers {
                    append("Authorization", "Bearer $accessToken")
                }
            }.body()
        }
    }
}
