package com.webasyst.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.webasyst.api.adapter.ListAdapter
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
    override val gson: Gson = GsonBuilder()
        .apply {
            configure(this@ApiClient.modules.mapNotNull { (_, factory) -> factory.gsonConfigurator })
        }
        .create()
    override val httpClient = HttpClient(engine) {
        install(JsonFeature) {
            serializer = GsonSerializer {
                configure(this@ApiClient.modules.mapNotNull { (_, factory) -> factory.gsonConfigurator })
            }
        }
    }

    /**
     * returns [ApiModuleFactory] for given [ApiModule] class or throws [IllegalStateException] if it is not configured
     */
    fun <T : ApiModule> getFactory(cls: Class<T>): ApiModuleFactory<*> =
        modules[cls] ?: throw IllegalArgumentException("Factory for $cls not found")

    fun configure(gsonBuilder: GsonBuilder) =
        gsonBuilder.configure(this@ApiClient.modules.mapNotNull { (_, factory) -> factory.gsonConfigurator })

    private fun GsonBuilder.configure(
        blocks: List<GsonBuilder.() -> Unit>
    ) {
        blocks.forEach(::apply)
        registerTypeAdapter(List::class.java, ListAdapter())
    }

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
