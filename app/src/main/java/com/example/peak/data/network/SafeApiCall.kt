package com.example.peak.data.network

import android.util.Log
import javax.net.ssl.SSLHandshakeException
import java.io.IOException
import java.net.SocketTimeoutException

data class SafeResult<T>(
    val data: T?,
    val source: Source,
    val error: Throwable? = null
)

enum class Source {
    NETWORK,
    CACHE,
    EMPTY,
    ERROR
}

object SafeApiCall {

    suspend fun <T> execute(
        tag: String = "SafeApiCall",
        call: suspend () -> T
    ): SafeResult<T> {
        return try {
            SafeResult(call(), Source.NETWORK)

        } catch (e: SSLHandshakeException) {
            Log.e(tag, "SSL error ignored: ${e.message}")
            SafeResult(null, Source.ERROR, e)

        } catch (e: SocketTimeoutException) {
            Log.e(tag, "Timeout ignored: ${e.message}")
            SafeResult(null, Source.ERROR, e)

        } catch (e: IOException) {
            Log.e(tag, "IO error ignored: ${e.message}")
            SafeResult(null, Source.ERROR, e)

        } catch (e: Exception) {
            Log.e(tag, "Unexpected error: ${e.message}")
            SafeResult(null, Source.ERROR, e)
        }
    }
}
