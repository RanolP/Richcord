package io.github.ranolp.richcord.util

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.pattern.color.ANSIConstants
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase

class ConsoleHighlighter : ForegroundCompositeConverterBase<ILoggingEvent>() {
    override fun getForegroundColorCode(event: ILoggingEvent): String =
        when (event.level) {
            Level.WARN -> ANSIConstants.YELLOW_FG
            Level.ERROR -> ANSIConstants.RED_FG
            Level.DEBUG -> ANSIConstants.CYAN_FG
            Level.TRACE -> ANSIConstants.GREEN_FG
            else -> ANSIConstants.WHITE_FG
        }
}
