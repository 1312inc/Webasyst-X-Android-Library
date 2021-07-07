package com.webasyst.api

import com.webasyst.api.util.GsonInstance
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature

/**
 * This class holds collection of all configured [api module factories][ApiModuleFactory]
 */
class ApiClient private constructor(
    override val clientId: String,
    modules: Map<Class<out ApiModule>, (config: ApiClientConfiguration, waidAuthenticator: WAIDAuthenticator) -> ApiModuleFactory<ApiModule>>,
    engine: HttpClientEngine,
    override val tokenCache: TokenCache,
    val waidAuthenticator: WAIDAuthenticator,
) : ApiClientConfiguration {
    val modules = modules
        .mapValues { (_, creator) -> creator.invoke(this, waidAuthenticator) }

    override val scope = this.modules
        .map { (_, factory) -> factory.scope }
    override val gson by GsonInstance
    override val httpClient = HttpClient(engine) {
        install(JsonFeature) {
            serializer = GsonSerializer {
                this.apply(GsonInstance::configureGsonBuilder)
            }
        }
    }

    /**
     * returns [ApiModuleFactory] for given [ApiModule] class or throws [IllegalStateException] if it is not configured
     */
    fun <T : ApiModule> getFactory(cls: Class<T>): ApiModuleFactory<*> =
        modules[cls] ?: throw IllegalArgumentException("Factory for $cls not found")

    class Builder {
        private val modules = mutableMapOf<Class<out ApiModule>, (config: ApiClientConfiguration, waidAuthenticator: WAIDAuthenticator) -> ApiModuleFactory<ApiModule>>()
        var clientId: String = ""
        var waidAuthenticator: WAIDAuthenticator? = null
        var httpClientEngine: HttpClientEngine? = null
        var tokenCache: TokenCache = TokenCacheRamImpl()

        fun <T: ApiModule> addModuleFactory(cls: Class<T>, factory: (config: ApiClientConfiguration, waidAuthenticator: WAIDAuthenticator) -> ApiModuleFactory<T>) {
            modules[cls] = factory
        }

        fun build(): ApiClient = ApiClient(
            clientId = clientId,
            modules = modules,
            waidAuthenticator = waidAuthenticator ?: throw IllegalStateException("WAID authenticator must be set"),
            engine = httpClientEngine ?: throw IllegalStateException("HttpClientEngine must be set"),
            tokenCache = tokenCache,
        )
    }

    companion object {
        operator fun invoke(block: Builder.() -> Unit): ApiClient =
            Builder()
                .apply(block)
                .also { builder ->
                    require(builder.clientId.isNotEmpty()) { "client_id must not be empty" }
                }
                .build()
    }
}
