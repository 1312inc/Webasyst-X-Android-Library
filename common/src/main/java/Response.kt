package com.webasyst.api

/**
 * Wraps [block] in try .. catch and returns it's result as [Response].
 */
inline fun <reified T> apiRequest(block: () -> T): Response<T> = try {
    Response.success(block())
} catch (e: Throwable) {
    Response.failure(e)
}

/**
 * This class represents generic api response. It can be either "successful" or "failed".
 *
 * Success means logical success i.e. server returned valid data or performed requested operation.
 * There were no business level errors, network failures, etc.
 *
 * _Failure_ means failed request (for any reason). Invalid input data, request not authenticated,
 * network failure etc.
 */
sealed class Response<out T> {
    /**
     * Returns server response for successful requests or throws [IllegalStateException] if request failed
     */
    open fun getSuccess(): T =
        throw IllegalStateException("getSuccess() can be called only on successful response")

    /**
     * Returns cause for failed requests or throws [IllegalStateException] if request was successful
     */
    open fun getFailureCause(): Throwable =
        throw IllegalStateException("getFailureCause() can be called only on failed response")

    /**
     * Helper function. [block] is called with successful server response.
     */
    open fun onSuccess(block: (value: T) -> Unit) : Response<T> = this

    /**
     * Helper function. [block] is called for failed request with failure cause.
     */
    open fun onFailure(block: (cause: Throwable) -> Unit) : Response<T> = this

    /**
     * Returns true for successful requests, false otherwise.
     */
    open fun isSuccess(): Boolean = false
    /**
     * Returns true for failed requests, false otherwise. Inverse of [isSuccess].
     */
    open fun isFailure(): Boolean = false

    private class Success<out T>(val value: T) : Response<T>() {
        override fun getSuccess(): T = value
        override fun isSuccess() = true
        override fun onSuccess(block: (value: T) -> Unit): Response<T> {
            block(value)
            return this
        }
    }

    private class Failure<out T>(val cause: Throwable) : Response<T>() {
        override fun getFailureCause(): Throwable = cause
        override fun isFailure(): Boolean = true
        override fun onFailure(block: (cause: Throwable) -> Unit): Response<T> {
            block(cause)
            return this
        }
    }

    companion object {
        /**
         * Creates successful [Response]
         */
        @JvmStatic
        fun <T> success(value: T): Response<T> = Success(value)
        /**
         * Creates failed [Response]
         */
        @JvmStatic
        fun <T> failure(cause: Throwable): Response<T> = Failure(cause)
    }
}
