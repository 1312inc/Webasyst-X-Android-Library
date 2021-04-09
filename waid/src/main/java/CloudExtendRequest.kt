package com.webasyst.waid

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.webasyst.api.adapter.DateAdapter
import java.util.Calendar

internal data class CloudExtendRequest(
    @SerializedName("client_id")
    val clientId: String,
    @SerializedName("expire_date")
    @JsonAdapter(DateAdapter::class)
    val expireDate: Calendar,
)
