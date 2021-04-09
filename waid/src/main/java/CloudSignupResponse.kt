package com.webasyst.waid

import com.google.gson.annotations.SerializedName

/**
 * Cloud signup (/id/api/v1/cloud/signup/) response
 */
data class CloudSignupResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("domain")
    val domain: String,
    @SerializedName("url")
    val url: String,
    @SerializedName("auth_endpoint")
    val authEndpoint: String
)
