package com.banno.gordon

import arrow.fx.IO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

internal fun <T> ioWithTimeout(
    timeoutMillis: Long,
    block: () -> T
): IO<T?> = IO {
    val deferred = CoroutineScope(Dispatchers.IO).async { block() }

    runBlocking {
        withTimeoutOrNull(timeoutMillis) {
            deferred.await()
        }
    }
}
