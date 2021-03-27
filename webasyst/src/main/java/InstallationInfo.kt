package com.webasyst.api.webasyst

import android.support.annotation.StringDef
import com.google.gson.annotations.SerializedName

data class InstallationInfo(
    @SerializedName("name")
    val name: String,
    @SerializedName("logo")
    val logo: Logo?
) {
    data class Logo(
        @LogoMode
        @SerializedName("mode")
        val mode: String,
        @SerializedName("text")
        val text: Text,
        @SerializedName("gradient")
        val gradient: Gradient,
        @SerializedName("two_lines")
        val twoLines: Boolean,

    ) {
        data class Text(
            @SerializedName("value")
            val value: String,
            @SerializedName("color")
            val color: String,
            @SerializedName("default_value")
            val defaultValue: String,
            @SerializedName("default_color")
            val defaultColor: String,
            @SerializedName("formatted_value")
            val formattedValue: String,
        )

        data class Gradient(
            @SerializedName("from")
            val from: String,
            @SerializedName("to")
            val to: String,
            @SerializedName("angle")
            val angle: String,
        )

        companion object {
            @Retention(AnnotationRetention.SOURCE)
            @StringDef(LOGO_MODE_GRADIENT)
            annotation class LogoMode
            const val LOGO_MODE_GRADIENT = "gradient"
        }
    }
}
