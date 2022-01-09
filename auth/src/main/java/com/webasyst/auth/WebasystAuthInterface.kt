package com.webasyst.auth

interface WebasystAuthInterface {
    suspend fun <T> withFreshAccessToken(task: AccessTokenTask<T>): T
}
