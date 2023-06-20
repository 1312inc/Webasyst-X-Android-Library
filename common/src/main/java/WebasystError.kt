package com.webasyst.api

import com.google.gson.annotations.SerializedName
import com.webasyst.api.util.GsonInstance
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import java.nio.charset.Charset

/**
 * Webasyst error response
 */
class WebasystError private constructor(
    @SerializedName(ERROR)
    val code: String,
    @SerializedName(ERROR_MESSAGE)
    val message: String,
) {
    var body: String? = null
        private set
    lateinit var httpCode: HttpStatusCode
        private set

    companion object {
        const val ERROR = "error"
        const val ERROR_DESCRIPTION = "error_description"
        const val ERROR_MESSAGE = "error_message"

        private val gson by GsonInstance

        private class NullableWebasystError(
            @SerializedName(ERROR)
            val code: String?,
            @SerializedName(ERROR_MESSAGE)
            val message: String?,
            @SerializedName(ERROR_DESCRIPTION)
            val description: String?,
        )

        operator fun invoke(body: String): WebasystError {
            try {
                val error = gson.fromJson(body, NullableWebasystError::class.java)
                val code = error.code
                var message = error.message ?: error.description
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

            val body = res.bodyAsText(Charset.forName("UTF-8"))
            return WebasystError(body).also {
                it.httpCode = res.status
                it.body = body
            }
        }
    }
}
