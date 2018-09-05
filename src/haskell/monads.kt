/** Helper monads that makes rewriting in Haskell easier */
package org.ice1000.ruiko.haskell

import kotlin.Nothing as Void

typealias Void = Void

sealed class Either<out T, out U> {
	fun asLeft() = (this as Left).obj
	fun asRight() = (this as Right).obj
	fun isLeft() = this is Left
	fun isRight() = this is Right
	fun asLeftOrNull() = (this as? Left)?.obj
	fun asRightOrNull() = (this as? Right)?.obj
	fun swap() = either(::Right, ::Left)
	inline fun <R> mapLeft(f: (T) -> R) = when (this) {
		is Left -> Left(f(obj))
		is Right -> Right(obj)
	}

	inline fun <R> mapRight(f: (U) -> R) = when (this) {
		is Left -> Left(obj)
		is Right -> Right(f(obj))
	}

	inline fun <R> either(f: (T) -> R, g: (U) -> R) = when (this) {
		is Left -> f(obj)
		is Right -> g(obj)
	}
}

inline fun <T, U, R> Either<T, U>.flatMapLeft(f: (T) -> Either<R, U>) = when (this) {
	is Left -> f(obj)
	is Right -> Right(obj)
}

inline fun <T, U, R> Either<T, U>.flatMapRight(f: (U) -> Either<T, R>) = when (this) {
	is Left -> Left(obj)
	is Right -> f(obj)
}

inline fun <T, U, R> Either<T, U>.flatMap(f: (T) -> Either<R, U>) = flatMapLeft(f)
inline fun <T, U, R> Either<T, U>.map(f: (T) -> R) = mapLeft(f)

data class Left<T>(val obj: T) : Either<T, Void>()
data class Right<U>(val obj: U) : Either<Void, U>()

fun <T : Any> T?.toMaybe() = this?.let(::Just) ?: Nothing

sealed class Maybe<out T> {
	fun asJust() = (this as Just).obj
	fun asJustSafe() = (this as? Just)?.obj
	fun isJust() = this is Just
	inline fun <R> map(f: (T) -> R) = flatMap(f compose ::Just)
	inline fun <R> flatMap(f: (T) -> Maybe<R>) = when (this) {
		is Just -> f(obj)
		Nothing -> Nothing
	}
}

infix fun <T> Maybe<T>.`||`(o: Maybe<T>) = if (isJust()) this else o
fun <T> Sequence<Maybe<T>>.firstJust() = fold(Nothing, Maybe<T>::`||`)
inline fun <T> Maybe<T>.getOr(f: () -> T) = asJustSafe() ?: f()
fun <T> Maybe<T>.getOr(t: T) = getOr { t }

data class Just<T>(val obj: T) : Maybe<T>()
object Nothing : Maybe<Void>()
