package com.webasyst.waid

import com.google.gson.annotations.SerializedName

/**
 * Cloud signup (/id/api/v1/cloud/signup/) request
 */
data class CloudSignup internal constructor(
    @SerializedName("plan_id")
    val planId: String? = null,
) {
    private constructor(builder: Builder) : this(
        planId = builder.planId,
    )

    class Builder internal constructor(
        var planId: String? = null,
    )

    companion object {
        operator fun invoke(build: Builder.() -> Unit): CloudSignup =
            CloudSignup(Builder().apply(build))
    }
}
