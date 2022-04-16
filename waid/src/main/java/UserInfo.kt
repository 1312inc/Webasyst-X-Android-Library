package com.webasyst.waid

import com.google.gson.annotations.SerializedName

data class UserInfo(
    @SerializedName("name")
    val name: String,
    @SerializedName("firstname")
    val firstName: String,
    @SerializedName("lastname")
    val lastName: String,
    @SerializedName("middlename")
    val middleName: String,
    @SerializedName("email")
    val email: List<Contact>,
    @SerializedName("phone")
    val phone: List<Contact>,
    @SerializedName("userpic")
    val userpic: String,
    @SerializedName("userpic_original_crop")
    val userpicOriginalCrop: String,
    @SerializedName("userpic_uploaded")
    val usrpicUploaded: Boolean
) {
    fun getEmail(): String = email.firstOrNull()?.value ?: ""

    data class Contact(
        @SerializedName("value")
        val value: String,
        @SerializedName("ext")
        val ext: String,
        @SerializedName("status")
        val status: String,
    )
}
