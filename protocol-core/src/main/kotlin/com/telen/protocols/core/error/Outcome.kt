package com.telen.protocols.core.error

/** Typed result channel: suspend functions return this directly, Flows emit it as values. */
sealed interface Outcome<out T, out E> {
    data class Success<T>(val value: T) : Outcome<T, Nothing>

    data class Failure<E>(val error: E) : Outcome<Nothing, E>
}
