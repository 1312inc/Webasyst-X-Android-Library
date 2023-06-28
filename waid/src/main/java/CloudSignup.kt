package com.webasyst.waid

import com.google.gson.annotations.SerializedName

/**
 * Cloud signup (/id/api/v1/cloud/signup/) request
 */
data class CloudSignup internal constructor(
    @SerializedName("plan_id")
    val planId: String? = null,
    @SerializedName("bundle")
    val bundle: String = "",
    @SerializedName("userdomain")
    val userdomain: String = "",
    @SerializedName("account_name")
    val accountName: String = ""

) {
    private constructor(builder: Builder) : this(
        planId = builder.planId,
        bundle = builder.bundle,
        userdomain = builder.userdomain,
        accountName = builder.accountName
    )

    class Builder internal constructor(
        var planId: String? = null,
        var bundle: String = "",
        var userdomain: String = "",
        var accountName: String = ""
    )

    companion object {
        operator fun invoke(build: Builder.() -> Unit): CloudSignup =
            CloudSignup(Builder().apply(build))
    }
}
