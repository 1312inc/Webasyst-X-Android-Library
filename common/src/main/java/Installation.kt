package com.webasyst.api

interface Installation {
    /**
     * Installation ID
     */
    val id: String

    /**
     * Installation URL
     */
    val urlBase: String

    companion object {
        /**
         * Creates [Installation] instance
         */
        operator fun invoke(
            id: String,
            urlBase: String,
        ): Installation = InstallationImpl(
            id = id,
            urlBase = urlBase,
        )
    }
}

private class InstallationImpl(
    override val id: String,
    override val urlBase: String,
) : Installation
