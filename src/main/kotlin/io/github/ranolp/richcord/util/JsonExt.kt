package io.github.ranolp.richcord.util

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import java.io.Reader
import java.nio.file.Path

val JSON_PARSER = JsonParser()
val PRETTY_PRINT_GSON = GsonBuilder().setPrettyPrinting().create()
val DEFAULT_GSON = GsonBuilder().create()

fun String.parseJson(): JsonElement = JSON_PARSER.parse(this)

fun Reader.parseJson(): JsonElement = JSON_PARSER.parse(this)

fun JsonElement.prettyPrint(): String = PRETTY_PRINT_GSON.toJson(this)

fun JsonElement.stringfy(): String = DEFAULT_GSON.toJson(this)

fun JsonElement.write(path: Path, prettyPrint: Boolean = true) =
    path.write(if (prettyPrint) prettyPrint() else stringfy())
