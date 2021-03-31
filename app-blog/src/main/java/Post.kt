package com.webasyst.api.blog

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.webasyst.api.adapter.DateTimeAdapter
import java.util.Calendar

data class Post(
    @SerializedName("id")
    val id: String,
    @SerializedName("datetime")
    @JsonAdapter(DateTimeAdapter::class)
    val dateTime: Calendar,
    @SerializedName("title")
    val title: String,
    @SerializedName("text")
    val text: String,
    @SerializedName("user")
    val user: User,
)
