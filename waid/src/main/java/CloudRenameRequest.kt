package com.webasyst.waid

import com.google.gson.annotations.SerializedName

class CloudRenameRequest(
    @SerializedName("client_id")
    val installationId: String,
    /**
     * First section of new cloud domain, eg. **w123456-1234**.webasyst.cloud
     */
    @SerializedName("domain")
    val cloudName: String,
)
