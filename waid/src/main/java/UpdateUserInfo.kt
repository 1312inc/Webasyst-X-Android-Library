package com.webasyst.waid

import com.google.gson.annotations.SerializedName

class UpdateUserInfo(
    @SerializedName("firstname")
    val firstName: String? = null,
    @SerializedName("lastname")
    val lastName: String? = null,
    @SerializedName("middlename")
    val middleName: String? = null,
    @SerializedName("email")
    val email: List<String>? = null,
    @SerializedName("phone")
    val phone: List<String>? = null,
)
