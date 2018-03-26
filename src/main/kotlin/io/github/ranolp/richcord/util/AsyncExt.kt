package io.github.ranolp.richcord.util

import java.util.concurrent.CompletableFuture

fun <T> async(lambda: () -> T?, whenComplete: (T?, Throwable?) -> Unit) {
    CompletableFuture.supplyAsync(lambda).whenComplete(whenComplete)
}

fun <T> async(lambda: () -> T?) {
    async(lambda) { _, _ -> }
}
