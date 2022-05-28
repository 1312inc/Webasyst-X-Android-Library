package com.webasyst.waid

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.webasyst.api.adapter.DateAdapter
import java.util.Calendar

data class Installation(
    @SerializedName("id")
    val id: String,
    @SerializedName("domain")
    val domain: String,
    @SerializedName("url")
    val url: String,
    @SerializedName("cloud_plan_id")
    val cloudPlanId: String?,
    @SerializedName("cloud_expire_date")
    @JsonAdapter(DateAdapter::class)
    val cloudExpireDate: Calendar?,
    @SerializedName("cloud_trial")
    val cloudTrial: Boolean?,
    @SerializedName("cloud_name")
    val cloudName: String?
)
