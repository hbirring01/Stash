package com.app.stash.android.data.ai

import java.util.concurrent.atomic.AtomicInteger

/**
 * Simple monotonic counter for capping LLM network calls within a single
 * processing pass. Constructed with an initial allowance; each [consume]
 * call subtracts one and returns whether the call is permitted.
 *
 * Thread-safe so multiple coroutines in a `coroutineScope { ... }` block can
 * share the same budget without losing accounting.
 */
class AiBudget(initial: Int) {
    private val remaining = AtomicInteger(initial.coerceAtLeast(0))

    /** Returns true and decrements if budget remains; otherwise returns false. */
    fun consume(): Boolean {
        while (true) {
            val current = remaining.get()
            if (current <= 0) return false
            if (remaining.compareAndSet(current, current - 1)) return true
        }
    }
}
