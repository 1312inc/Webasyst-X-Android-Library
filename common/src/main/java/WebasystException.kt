package com.webasyst.api

import io.ktor.client.statement.HttpResponse
import java.util.Locale
import java.util.ResourceBundle

class WebasystException(
    val webasystCode: String,
    val webasystMessage: String,
    val webasystApp: String,
    val webasystHost: String,
    cause: Throwable? = null,
) : Throwable(webasystMessage, cause) {
    /**
     * Returns localized error message.
     * Returned message is suitable for display to the user.
     */
    override fun getLocalizedMessage(): String {
        val locale = Locale.getDefault()
        val stringResources = ResourceBundle.getBundle("strings", locale)
        val key =
            if (stringResources.containsKey(errorCodes[webasystCode])) errorCodes[webasystCode]
            else errorCodes[UNRECOGNIZED_ERROR]
        return stringResources
            .getString(key)
            .format(webasystApp, webasystHost)
    }

    companion object {
        suspend operator fun invoke(
            response: HttpResponse,
            cause: Throwable?,
            webasystApp: String,
            webasystHost: String,
        ): WebasystException {
            val error = WebasystError(response)
            return WebasystException(
                webasystCode = error.code,
                webasystMessage = error.message,
                webasystApp = webasystApp,
                webasystHost = webasystHost,
                cause = cause,
            )
        }

        /** Unrecognized error */
        const val UNRECOGNIZED_ERROR = ""
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

        val errorCodes = mapOf(
            UNRECOGNIZED_ERROR to "webasyst_error_generic",
            ERROR_INVALID_ERROR_OBJECT to "webasyst_error_invalid_error_object",
            ERROR_CODE_INVALID_CLIENT to "waid_error_invalid_client",
            ERROR_CONNECTION_FAILED to "webasyst_error_connection_failed",
            ERROR_APP_NOT_INSTALLED to "webasyst_error_app_not_installed",
            ERROR_DISABLED to "webasyst_error_disabled",
        )
    }
}
