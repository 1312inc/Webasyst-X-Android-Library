package com.webasyst.api.adapter

import android.util.Log
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.webasyst.api.util.threadLocal
import java.lang.reflect.Type
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * JSON adapter that supports String (yyyy-MM-DD HH:mm:ss) <-> [Calendar] conversion
 */
class DateTimeAdapter : JsonDeserializer<Calendar>, JsonSerializer<Calendar> {
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

    override fun serialize(
        src: Calendar?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement =
        if (src == null) JsonNull.INSTANCE
        else JsonPrimitive(dateFormatWithTime.format(src.time))

    companion object {
        private const val TAG = "webasyst_api"
        private const val DATE_FORMAT = "yyyy-MM-DD HH:mm:ss"
    }
}
