package com.webasyst.api

import com.webasyst.api.util.GsonInstance
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiClientTest {
    @Test
    fun `test that api ApiClient returns appropriate errors`() {
        val engine = MockEngine.create {
            addHandler {
                this.respond(
                    content = """{"error":"disabled","error_description":"Использование мобильного приложения недоступно на вашем тарифном плане."}""",
                    status = HttpStatusCode.NotFound,
                    headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                )
            }
        }
        val client = ApiClient.createHttpClient(engine)

        val config = object : ApiClientConfiguration {
            override val clientId get() = "mock"
            override val httpClient = client
            override val gson by GsonInstance
            override val tokenCache = TokenCacheRamImpl(Long.MAX_VALUE)
            override val scope = listOf("mock")
        }

        val installation = object : Installation {
            override val id get() = "mock"
            override val urlBase get() = "mock"
        }

        val authenticator = object : WAIDAuthenticator {
            override suspend fun getInstallationApiAuthCodes(appClientIDs: Set<String>): Response<Map<String, String>> {
                return apiRequest {
                    appClientIDs.associateWith {
                        "mock"
                    }
                }
            }
        }

        val apiModule = object : ApiModule(
            config = config,
            installation = installation,
            waidAuthenticator = authenticator,
        ) {
            override val appName get() = "mock"
        }

        runBlocking {
            try {
                apiModule.getToken()
            } catch (e: Throwable) {
                assertTrue(
                    e is WebasystException,
                    "Thrown exception should be an instance of WebasystException"
                )
                assertEquals(
                    WebasystException.ERROR_DISABLED,
                    e.webasystCode,
                    "Error code should be ${WebasystException.ERROR_DISABLED}"
                )
            }
        }
    }
}
