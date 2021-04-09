package com.webasyst.api.util

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.io.Reader
import java.nio.charset.Charset

fun <T> ByteReadChannel.useReader(charset: Charset = Charset.forName("UTF8"), block: (Reader) -> T): T =
    toInputStream().use {
        it.reader(charset).use(block)
    }
