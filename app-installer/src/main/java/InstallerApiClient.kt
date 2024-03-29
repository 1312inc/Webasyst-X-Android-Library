package com.webasyst.api.blog

import com.webasyst.api.ApiClientConfiguration
import com.webasyst.api.ApiModule
import com.webasyst.api.Installation
import com.webasyst.api.WAIDAuthenticator
import com.webasyst.api.util.useReader
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.parameters

class InstallerApiClient(
    config: ApiClientConfiguration,
    installation: Installation,
    waidAuthenticator: WAIDAuthenticator,
) : ApiModule(
    config = config,
    installation = installation,
    waidAuthenticator = waidAuthenticator,
) {
    override val appName get() = SCOPE

    suspend fun install(slug: String): InstallerResponse = try {
        val url: URLBuilder = URLBuilder("$urlBase/api.php/installer.product.install").apply {
            parameters.apply {
                set("access_token", getToken().token)
                set("format", "json")
            }
        }

        val response = performRequest {
            method = HttpMethod.Post
            url(url.build())
            setBody(
                FormDataContent(
                    parameters {
                        append("slug", slug)
                    }
                )
            )
        }

        if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
            InstallerResponse.Success
        } else {
            response.bodyAsChannel().useReader { responseReader ->
                gson.fromJson(responseReader, InstallerResponse.Error::class.java)
            }
        }
    } catch (e: Throwable) {
        InstallerResponse.NetworkError(e)
    }

    companion object {
        const val SCOPE = "installer"
    }
}
