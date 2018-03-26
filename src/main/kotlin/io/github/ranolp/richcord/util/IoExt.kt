package io.github.ranolp.richcord.util

import java.io.BufferedReader
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun String.toPath(vararg more: String): Path = Paths.get(this, *more)

fun Path.newBufferedReader(cs: Charset = Charsets.UTF_8): BufferedReader = Files.newBufferedReader(this, cs)

fun Path.newInputStream(): InputStream = Files.newInputStream(this)

fun Path.readAllBytes(): ByteArray = Files.readAllBytes(this)

fun Path.createFile(): Path = Files.createFile(this)

fun Path.exists(): Boolean = toFile().exists()

fun Path.write(byteArray: ByteArray): Path = Files.write(this, byteArray)

fun Path.write(string: String, charset: Charset = Charsets.UTF_8): Path = Files.write(this, string.toByteArray(charset))

val Path.name: String
    get() = toFile().name

val Path.extension: String
    get() = toFile().extension
