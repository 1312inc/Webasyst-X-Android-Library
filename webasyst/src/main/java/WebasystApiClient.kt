package com.webasyst.api.webasyst

import com.webasyst.api.ApiClientConfiguration
import com.webasyst.api.ApiModule
import com.webasyst.api.Installation
import com.webasyst.api.Response
import com.webasyst.api.WAIDAuthenticator

class WebasystApiClient(
    config: ApiClientConfiguration,
    installation: Installation,
    waidAuthenticator: WAIDAuthenticator,
) : ApiModule(
    config = config,
    installation = installation,
    waidAuthenticator = waidAuthenticator,
) {
    override val appName get() = SCOPE

    suspend fun getInstallationInfo(): Response<InstallationInfo> =
        get("$urlBase/api.php/webasyst.getInfo")

    companion object {
        const val SCOPE = "webasyst"
    }
}
