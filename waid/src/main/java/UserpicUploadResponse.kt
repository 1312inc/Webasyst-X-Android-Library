package com.webasyst.waid

import com.google.gson.annotations.SerializedName

class UserpicUploadResponse(
    @SerializedName("userpic")
    val userpic: String,
    @SerializedName("userpic_original_crop")
    val userpicOriginalCrop: String,
)
