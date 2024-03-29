package com.webasyst.api

import com.google.gson.Gson
import io.ktor.client.HttpClient

interface ApiClientConfiguration {
    /**
     * Application ID
     */
    val clientId: String
    val httpClient: HttpClient
    val gson: Gson
    val tokenCache: TokenCache
    val scope: List<String>
}
