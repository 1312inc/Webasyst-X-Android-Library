package com.webasyst.api.blog

import com.webasyst.api.ApiClientConfiguration
import com.webasyst.api.ApiModuleFactory
import com.webasyst.api.Installation
import com.webasyst.api.WAIDAuthenticator

class InstallerApiClientFactory(
    private val config: ApiClientConfiguration,
    private val waidAuthenticator: WAIDAuthenticator,
) : ApiModuleFactory<InstallerApiClient>() {
    override val scope = InstallerApiClient.SCOPE

    override fun instanceForInstallation(installation: Installation): InstallerApiClient {
        return InstallerApiClient(
            config = config,
            waidAuthenticator = waidAuthenticator,
            installation = installation,
        )
    }
}
