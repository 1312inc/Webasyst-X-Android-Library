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
        @SerializedName("two_lines")
        val twoLines: Boolean,
        @SerializedName("gradient")
        val gradient: Gradient,
        @SerializedName("image")
        val image: Image?,
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

        data class Image(
            @SerializedName("original")
            val original: Original,
            @SerializedName("thumbs")
            val thumbs: Map<String, Thumb>,

        ) {
            data class Original(
                @SerializedName("path")
                val path: String,
                @SerializedName("name")
                val name: String,
                @SerializedName("ext")
                val ext: String,
                @SerializedName("ts")
                val ts: Int,
                @SerializedName("url")
                val url: String,
            )

            data class Thumb(
                @SerializedName("path")
                val path: String,
                @SerializedName("ts")
                val ts: Int,
                @SerializedName("url")
                val url: String,
            )
        }


        companion object {
            @Retention(AnnotationRetention.SOURCE)
            @StringDef(LOGO_MODE_GRADIENT, LOGO_MODE_IMAGE)
            annotation class LogoMode
            const val LOGO_MODE_GRADIENT = "gradient"
            const val LOGO_MODE_IMAGE = "image"
        }
    }
}
