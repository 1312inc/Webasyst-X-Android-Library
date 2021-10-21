package com.webasyst.api.blog

import com.google.gson.annotations.SerializedName

sealed class InstallerResponse {
    object Success : InstallerResponse()

    class Error(
        @SerializedName("error")
        val error: String,
        @SerializedName("error_description")
        val errorDescription: String,
    ) : InstallerResponse()

    class NetworkError(val cause: Throwable) : InstallerResponse()
}
