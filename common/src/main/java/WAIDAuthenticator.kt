package com.webasyst.api

interface WAIDAuthenticator {
    /**
     * Requests authentication token(s).
     * @param appClientIDs Application IDs to request tokens for
     * @return application IDs mapped to access tokens
     */
    suspend fun getInstallationApiAuthCodes(appClientIDs: Set<String>): Response<Map<String, String>>
}
