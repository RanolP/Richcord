package io.github.ranolp.richcord.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline fun <reified T> T.logger(): Logger = LoggerFactory.getLogger(T::class.java)

inline fun <reified T> logger(): Logger = LoggerFactory.getLogger(T::class.java)

val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
