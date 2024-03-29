package com.webasyst.api

import com.google.gson.annotations.SerializedName

/**
 * Access token response
 */
data class AccessToken(
    @SerializedName("access_token")
    val token: String,
    @SerializedName("error")
    val error: String?,
    @SerializedName("error_description")
    val errorDescription: String?
)
