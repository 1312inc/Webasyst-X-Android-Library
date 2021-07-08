package com.webasyst.api

import org.junit.Test
import java.util.ResourceBundle
import kotlin.test.assertTrue

class WebasystExceptionStringResourceTest {
    @Test
    fun `test that error messages are available in all locales`() {
        val noFallbackBundleControl = ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES)
        val bundleNames = listOf(
            "strings",
            "strings_ru",
        )
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
}
