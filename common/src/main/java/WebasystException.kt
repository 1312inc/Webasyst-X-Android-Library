package com.webasyst.api

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
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

    companion object {
        suspend operator fun invoke(
            apiModule: ApiModule,
            response: HttpResponse,
            cause: Throwable?,
        ): WebasystException {
            val webasystError = WebasystError(response)
            return WebasystException(
                webasystCode = webasystError.code,
                webasystMessage = webasystError.message,
                webasystApp = apiModule.appName,
                webasystHost = apiModule.urlBase,
                responseStatusCode = webasystError.httpCode.value,
                responseBody = webasystError.body,
                cause = cause,
            )
        }

        suspend operator fun invoke(
            webasystApp: String,
            webasystHost: String,
            response: HttpResponse?,
            cause: Throwable?,
        ): WebasystException {
            val webasystError = WebasystError(response)
            return WebasystException(
                webasystCode = webasystError.code,
                webasystMessage = webasystError.message,
                webasystApp = webasystApp,
                webasystHost = webasystHost,
                responseStatusCode = webasystError.httpCode.value,
                responseBody = webasystError.body,
                cause = cause,
            )
        }

        suspend operator fun invoke(
            apiModule: ApiModule,
            response: HttpResponse?,
            token: AccessToken?,
            cause: Throwable?,
        ): WebasystException {
            return WebasystException(
                webasystCode = token?.error ?: "",
                webasystMessage = token?.errorDescription ?: "Unknown WAID error",
                webasystApp = apiModule.appName,
                webasystHost = apiModule.urlBase,
                responseStatusCode = response?.status?.value,
                responseBody = response?.readText(),
                cause = cause,
            )
        }

        operator fun invoke(
            errorCode: String?,
            apiModule: ApiModule,
            cause: Throwable?,
        ): WebasystException {
            return WebasystException(
                webasystCode = errorCode ?: UNRECOGNIZED_WAID_ERROR,
                webasystMessage = "Failed to connect to ${apiModule.urlBase}",
                webasystApp = apiModule.appName,
                webasystHost = apiModule.urlBase,
                responseStatusCode = 0,
                responseBody = null,
                cause = cause,
            )
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
