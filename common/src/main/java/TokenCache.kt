package com.webasyst.api

/**
 * Authentication token holder
 */
interface TokenCache {
    /**
     * Returns [AccessToken] associated with given [url] and [scope] or null if it does not exist
     */
    suspend fun get(url: String, scope: String): AccessToken?

    /**
     * Sets new [AccessToken] for given [url] and [scope]
     */
    suspend fun set(url: String, scope: String, token: AccessToken)

    /**
     * Removes all tokens from this cache instance
     */
    suspend fun clear()
}

