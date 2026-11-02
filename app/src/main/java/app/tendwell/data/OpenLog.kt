package app.tendwell.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** What happened at a pause screen. */
enum class PauseOutcome(val code: String) {
    Opened("o"),
    TurnedBack("b");

    companion object {
        fun of(code: String) = entries.firstOrNull { it.code == code }
    }
}

data class OpenEvent(val epochMs: Long, val outcome: PauseOutcome, val appKey: String)

/**
 * A small append-only log of pause-screen outcomes for paused apps only, kept
 * in the app's private files on the phone. It powers "7th open today" now and
 * weekly insights later. Entries older than [KEEP_DAYS] days are dropped.
 */
class OpenLog(context: Context) {
    private val file = File(context.applicationContext.filesDir, "opens.log")
    private val lock = Mutex()

    suspend fun record(appKey: String, outcome: PauseOutcome, now: Long = System.currentTimeMillis()) =
        withContext(Dispatchers.IO) {
            lock.withLock { file.appendText(OpenLogFormat.line(OpenEvent(now, outcome, appKey)) + "\n") }
        }

    suspend fun eventsSince(sinceMs: Long): List<OpenEvent> = withContext(Dispatchers.IO) {
        lock.withLock {
            if (!file.exists()) emptyList()
            else file.readLines().mapNotNull(OpenLogFormat::parse).filter { it.epochMs >= sinceMs }
        }
    }

    suspend fun prune(now: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        lock.withLock {
            if (!file.exists()) return@withLock
            val cutoff = now - KEEP_DAYS * 24 * 60 * 60 * 1000L
            val kept = file.readLines().filter { line -> OpenLogFormat.parse(line)?.let { it.epochMs >= cutoff } ?: false }
            file.writeText(if (kept.isEmpty()) "" else kept.joinToString("\n", postfix = "\n"))
        }
    }

    companion object {
        const val KEEP_DAYS = 35
    }
}

/** The log's line format, "epochMs,outcome,appKey". Kept separate so it can be unit-tested. */
object OpenLogFormat {
    fun line(e: OpenEvent): String = "${e.epochMs},${e.outcome.code},${e.appKey}"

    fun parse(line: String): OpenEvent? {
        val parts = line.split(',', limit = 3)
        if (parts.size != 3) return null
        val ms = parts[0].toLongOrNull() ?: return null
        val outcome = PauseOutcome.of(parts[1]) ?: return null
        if (parts[2].isBlank()) return null
        return OpenEvent(ms, outcome, parts[2])
    }

    /** How many times the app was actually opened in [fromMs, toMs). */
    fun opensBetween(events: List<OpenEvent>, appKey: String, fromMs: Long, toMs: Long): Int =
        events.count { it.appKey == appKey && it.outcome == PauseOutcome.Opened && it.epochMs in fromMs until toMs }
}
