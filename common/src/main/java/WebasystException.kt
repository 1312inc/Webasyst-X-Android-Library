package com.webasyst.api

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import java.util.Locale
import java.util.ResourceBundle

class WebasystException internal constructor(
    val webasystCode: String,
    val webasystMessage: String,
    val webasystApp: String,
    val webasystHost: String,
    val responseStatusCode: Int?,
    val responseBody: String?,
    cause: Throwable? = null,
) : Throwable(webasystMessage, cause) {
    /**
     * Returns localized error message.
     * Returned message is suitable for display to the user.
     */
    override fun getLocalizedMessage(): String {
        if (webasystCode == ERROR_CODE_INVALID_CLIENT) {
            return webasystMessage
        }

        val locale = Locale.getDefault()
        val stringResources = ResourceBundle.getBundle("strings", locale)
        val code = errorCodes[webasystCode] ?: errorCodes[UNRECOGNIZED_ERROR]
        val key =
            if (stringResources.containsKey(code)) code
            else errorCodes[UNRECOGNIZED_ERROR]
        return stringResources
            .getString(key)
            .format(webasystApp, webasystHost)
    }

    class Builder(
        var webasystCode: String? = null,
        var webasystMessage: String? = null,
        var webasystApp: String? = null,
        var webasystHost: String? = null,
        var responseStatusCode: Int? = null,
        var responseBody: String? = null,
        var cause: Throwable? = null,
    ) {
        fun withApiModule(apiModule: ApiModuleInfo): Builder {
            webasystApp = apiModule.appName
            webasystHost = apiModule.urlBase
            return this
        }

        fun withErrorInfo(errorCode: String, errorMessage: String): Builder {
            webasystCode = errorCode
            webasystMessage = errorMessage
            return this
        }

        fun withCause(cause: Throwable): Builder {
            this.cause = cause
            return this
        }

        /**
         * Pulls all available data from [response]:
         *
         * - [responseStatusCode]
         * - [responseBody]
         * - [webasystCode] and [webasystMessage] if response is well-formed [WebasystError]. Sets [webasystCode] to [ERROR_INVALID_ERROR_OBJECT] otherwise.
         *
         * Returns this [Builder] instance to fulfill *builder pattern*.
         */
        suspend fun withHttpResponse(response: HttpResponse): Builder {
            responseStatusCode = response.status.value
            responseBody = response.bodyAsText()
            if (null != responseBody) {
                try {
                    val error = WebasystError(responseBody!!)
                    webasystCode = error.code
                    webasystMessage = error.message
                } catch (e: Throwable) {
                    webasystCode = ERROR_INVALID_ERROR_OBJECT
                    webasystMessage = "Received an invalid error object"
                }
            }
            return this
        }

        fun build(): WebasystException {
            return WebasystException(
                webasystCode = webasystCode ?: UNRECOGNIZED_ERROR,
                webasystMessage = webasystMessage ?: "",
                webasystApp = webasystApp ?: "",
                webasystHost = webasystHost ?: "",
                responseStatusCode = responseStatusCode,
                responseBody = responseBody,
                cause = cause,
            )
        }
    }

    companion object {
        suspend operator fun invoke(block: suspend Builder.() -> Unit): WebasystException {
            val builder = Builder()
            block.invoke(builder)
            return builder.build()
        }

        /** Unrecognized error */
        const val UNRECOGNIZED_ERROR = ""
        /** Unrecognized WAID error */
        const val UNRECOGNIZED_WAID_ERROR = "waid_error"
        /** Invalid error object received from server */
        const val ERROR_INVALID_ERROR_OBJECT = "invalid_error_object"
        /** Indicated invalid Client ID */
        const val ERROR_CODE_INVALID_CLIENT = "invalid_client"
        /** Connection failed */
        const val ERROR_CONNECTION_FAILED = "connection_failed"
        /** Application not installed */
        const val ERROR_APP_NOT_INSTALLED = "app_not_installed"
        /** Api is disabled altogether */
        const val ERROR_DISABLED = "disabled"
        /** Account suspended */
        const val ERROR_ACCOUNT_SUSPENDED = "account_suspended"

        val errorCodes = mapOf(
            UNRECOGNIZED_WAID_ERROR to "waid_error_generic",
            UNRECOGNIZED_ERROR to "webasyst_error_generic",
            ERROR_INVALID_ERROR_OBJECT to "webasyst_error_invalid_error_object",
            ERROR_CODE_INVALID_CLIENT to "waid_error_invalid_client",
            ERROR_CONNECTION_FAILED to "webasyst_error_connection_failed",
            ERROR_APP_NOT_INSTALLED to "webasyst_error_app_not_installed",
            ERROR_ACCOUNT_SUSPENDED to "webasyst_error_account_suspended",
            ERROR_DISABLED to "webasyst_error_disabled",
        )
    }
}
