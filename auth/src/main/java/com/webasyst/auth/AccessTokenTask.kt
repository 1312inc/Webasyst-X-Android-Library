package com.webasyst.auth

interface AccessTokenTask<T> {
    suspend fun apply(accessToken: String?, exception: Throwable?): T
}
