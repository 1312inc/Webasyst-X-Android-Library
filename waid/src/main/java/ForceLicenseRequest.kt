package com.webasyst.waid

import com.google.gson.annotations.SerializedName

internal data class ForceLicenseRequest(
    @SerializedName("client_id")
    val clientId: String,
    @SerializedName("slug")
    val slug: String,
)
