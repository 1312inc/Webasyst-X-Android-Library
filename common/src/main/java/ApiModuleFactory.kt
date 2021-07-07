package com.webasyst.api

/**
 * Abstract [ApiModule] factory. Concrete
 */
abstract class ApiModuleFactory<out T : ApiModule> {
    /**
     * Application slug
     */
    abstract val scope: String

    /**
     * Creates [ApiModule] instance tied to given [installation]
     */
    abstract fun instanceForInstallation(installation: Installation): T
}
