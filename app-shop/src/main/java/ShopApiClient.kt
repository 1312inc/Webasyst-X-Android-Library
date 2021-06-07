package com.webasyst.api.shop

import com.webasyst.api.ApiClientConfiguration
import com.webasyst.api.ApiModule
import com.webasyst.api.Installation
import com.webasyst.api.Response
import com.webasyst.api.WAIDAuthenticator
import com.webasyst.api.apiRequest
import io.ktor.client.request.parameter

class ShopApiClient(
    config: ApiClientConfiguration,
    installation: Installation,
    waidAuthenticator: WAIDAuthenticator,
) : ApiModule(
    config = config,
    installation = installation,
    waidAuthenticator = waidAuthenticator,
) {
    override val appName get() = SCOPE

    suspend fun getOrders(): Response<OrderList> = apiRequest {
        return client.doGet("$urlBase/api.php/shop.order.search") {
            parameter("limit", 10)
        }
    }

    companion object {
        const val SCOPE = "shop"
    }
}
