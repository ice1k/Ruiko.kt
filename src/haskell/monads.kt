/** Helper monads that makes rewriting in Haskell easier */
package org.ice1000.ruiko.haskell

sealed class Either<out T, out U> {
	fun asLeft() = (this as Left).obj
	fun asRight() = (this as Right).obj
	fun isLeft() = this is Left
	fun isRight() = this is Right
	fun asLeftOrNull() = (this as? Left)?.obj
	fun asRightOrNull() = (this as? Right)?.obj
	fun swap() = either(::Right, ::Left)
	inline infix fun <R> mapLeft(f: (T) -> R) = when (this) {
		is Left -> Left(f(obj))
		is Right -> Right(obj)
	}

	inline infix fun <R> mapRight(f: (U) -> R) = when (this) {
		is Left -> Left(obj)
		is Right -> Right(f(obj))
	}

	inline fun <R> either(f: (T) -> R, g: (U) -> R) = when (this) {
		is Left -> f(obj)
		is Right -> g(obj)
	}
}

inline infix fun <T, U, R> Either<T, U>.flatMapLeft(f: (T) -> Either<R, U>) = when (this) {
	is Left -> f(obj)
	is Right -> Right(obj)
}

inline infix fun <T, U, R> Either<T, U>.flatMapRight(f: (U) -> Either<T, R>) = when (this) {
	is Left -> Left(obj)
	is Right -> f(obj)
}

inline infix fun <T, U, R> Either<T, U>.flatMap(f: (T) -> Either<R, U>) = flatMapLeft(f)
inline infix fun <T, U, R> Either<T, U>.map(f: (T) -> R) = mapLeft(f)

data class Left<T>(val obj: T) : Either<T, Nothing>()
data class Right<U>(val obj: U) : Either<Nothing, U>()

fun <T : Any> T?.toMaybe() = this?.let(::Some) ?: None

sealed class Option<out T> {
	fun asJust() = (this as Some).obj
	fun asJustSafe() = (this as? Some)?.obj
	fun isJust() = this is Some
	inline infix fun <R> map(f: (T) -> R) = flatMap(f `-*` ::Some)
	inline infix fun <R> flatMap(f: (T) -> Option<R>) = when (this) {
		is Some -> f(obj)
		None -> None
	}
}

infix fun <T> Option<T>.`||`(o: Option<T>) = if (isJust()) this else o
fun <T> Sequence<Option<T>>.firstJust() = fold(None, Option<T>::`||`)
fun <T> Iterable<Option<T>>.firstJust() = fold(None, Option<T>::`||`)
inline infix fun <T> Option<T>.getOr(f: () -> T) = asJustSafe() ?: f()
infix fun <T> Option<T>.getOr(t: T) = getOr { t }

data class Some<T>(val obj: T) : Option<T>()
object None : Option<Nothing>()
