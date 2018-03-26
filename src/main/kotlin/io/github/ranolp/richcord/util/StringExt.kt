package io.github.ranolp.richcord.util

import java.nio.charset.Charset
import java.util.*

fun String.encodeBase64(charset: Charset = Charsets.UTF_8): String = toByteArray(charset).encodeBase64()

fun ByteArray.encodeBase64(): String = Base64.getEncoder().encodeToString(this)
