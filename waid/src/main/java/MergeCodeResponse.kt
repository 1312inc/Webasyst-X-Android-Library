package com.webasyst.waid

import com.google.gson.annotations.SerializedName

class MergeCodeResponse(
    @SerializedName("code")
    val code: String,
    @SerializedName("expires")
    val expires: Long,
)
