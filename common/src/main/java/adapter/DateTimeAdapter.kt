package com.webasyst.api.adapter

import android.util.Log
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.webasyst.api.util.threadLocal
import java.lang.reflect.Type
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class DateTimeAdapter : JsonDeserializer<Calendar> {
    private val dateFormatWithTime by threadLocal<DateFormat> {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD) {
            SimpleDateFormat(DATE_FORMAT, Locale.ROOT)
        } else {
            SimpleDateFormat(DATE_FORMAT)
        }
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Calendar =
        Calendar.getInstance().also { calendar ->
            try {
                calendar.time = dateFormatWithTime.parse(json.asString)!!
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to parse date ($json)", e)
            }
        }

    companion object {
        private const val TAG = "webasyst_api"
        private const val DATE_FORMAT = "yyyy-MM-DD HH:mm:ss"
    }
}
