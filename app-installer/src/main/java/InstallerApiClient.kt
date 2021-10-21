package com.webasyst.api.blog

import com.webasyst.api.ApiClientConfiguration
import com.webasyst.api.ApiModule
import com.webasyst.api.Installation
import com.webasyst.api.WAIDAuthenticator
import com.webasyst.api.util.useReader
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder

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

        val response = client.post<HttpResponse>(url.build()) {
            body = FormDataContent(Parameters.build {
                set("slug", slug)
            })
        }

        if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
            InstallerResponse.Success
        } else {
            response.content.useReader { responseReader ->
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
