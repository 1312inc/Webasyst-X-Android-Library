package com.webasyst.api

import org.junit.Test
import java.util.ResourceBundle
import kotlin.test.assertTrue

class WebasystExceptionStringResourceTest {
    private val noFallbackBundleControl = ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES)
    private val bundleNames = listOf(
        "strings",
        "strings_ru",
    )

    @Test
    fun `test that all error messages are available`() {
        val bundle = ResourceBundle.getBundle(bundleNames.first(), noFallbackBundleControl)
        WebasystException
            .errorCodes
            .values
            .forEach { key ->
                assertTrue(bundle.containsKey(key), "[${bundle.baseBundleName}.properties] does not contain $key")
            }
    }

    @Test
    fun `test that error messages are available in all locales`() {
        val bundles = bundleNames.map { name ->
            ResourceBundle.getBundle(name, noFallbackBundleControl)
        }
        val keys = bundles
            .map { it.keys.toList() }
            .flatten()
            .toSet()
        bundles.forEach { bundle ->
            keys.forEach { key ->
                assertTrue(bundle.containsKey(key), "[${bundle.baseBundleName}.properties] does not contain $key")
            }
        }
    }

    @Test
    fun `test that WebasystException_getLocalizedMessage() does not throw NPE if webasystCode is unrecognized`() {
        val e = WebasystException(
            webasystCode = "abracadabra",
            webasystMessage = "",
            webasystApp = "app",
            webasystHost = "example.com",
            responseStatusCode = null,
            responseBody = null,
            cause = null,
        )

        e.localizedMessage
    }
}
