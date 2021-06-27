package com.webasyst.waid

import com.google.gson.annotations.SerializedName

class HeadlessTokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("expires_in")
    val expiresIn: Long,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("token_type")
    val tokenType: String,
)
