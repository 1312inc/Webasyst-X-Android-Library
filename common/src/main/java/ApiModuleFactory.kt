package com.webasyst.api

import com.google.gson.GsonBuilder

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

    /**
     * Special [Gson] configuration needed by this [ApiModule] can be done by overriding this
     */
    open val gsonConfigurator: ((GsonBuilder) -> Unit)? = null
}
