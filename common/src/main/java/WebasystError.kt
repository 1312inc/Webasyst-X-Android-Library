package com.webasyst.api

import com.google.gson.annotations.SerializedName
import com.webasyst.api.util.GsonInstance
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.HttpStatusCode
import java.nio.charset.Charset

/**
 * Webasyst error response
 */
class WebasystError private constructor(
    @SerializedName("error")
    val code: String,
    @SerializedName("error_description")
    val message: String,
) {
    var body: String? = null
        private set
    lateinit var httpCode: HttpStatusCode
        private set

    companion object {
        private val gson by GsonInstance

        private class NullableWebasystError(
            @SerializedName("error")
            val code: String?,
            @SerializedName("error_description")
            val message: String?,
        )

        operator fun invoke(body: String): WebasystError {
            try {
                val error = gson.fromJson(body, NullableWebasystError::class.java)
                val code = error.code
                var message = error.message
                if (code == null) {
                    throw IllegalStateException()
                } else if (message == null) {
                    message = code
                }
                return WebasystError(code = code, message = message)
            } catch (e: Throwable) {
                return WebasystError(
                    code = WebasystException.ERROR_INVALID_ERROR_OBJECT,
                    message = "Malformed error received from server."
                )
            }
        }

        /**
         * Creates new [WebasystError] instance from given [HttpResponse]
         */
        suspend operator fun invoke(res: HttpResponse?): WebasystError {
            if (null == res) {
                return WebasystError(
                    WebasystException.ERROR_CONNECTION_FAILED,
                    "Connection failed"
                )
            }

            val body = res.readText(Charset.forName("UTF-8"))
            return WebasystError(body).also {
                it.httpCode = res.status
                it.body = body
            }
        }
    }
}
