package org.ice1000.ruiko.core

import org.ice1000.ruiko.haskell.Maybe
import org.ice1000.ruiko.lexer.Lexer

typealias RewriteFunc<T> = (State<T>) -> (Ast<T>) -> Ast<T>

data class LiteralRule(
		val test: (Token) -> Boolean,
		val lexer: Maybe<() -> Lexer>
)

class Trace<T> // TODO ask red red wat is dis UwU
data class LRInternal(val depth: Int, val name: String)
data class State<T>(
		var lr: HashSet<LRInternal>,
		var context: T,
		val trace: Trace<Trace<String>>,
		val lang: HashMap<String, Parser<T>>
)

sealed class Parser<out T>
data class Predicate<T>(val f: (State<T>) -> Boolean) : Parser<T>()
data class Rewrite<T>(val p: Parser<T>, val r: RewriteFunc<T>) : Parser<T>()
data class Literal(val lit: LiteralRule) : Parser<Nothing>()
object Anything : Parser<Nothing>()
data class Lens<T>(val f: (T) -> (Ast<T>) -> T, val p: Parser<T>) : Parser<T>()
data class Named<T>(val name: String, val f: () -> T) : Parser<T>()
data class And<T>(val list: List<Parser<T>>) : Parser<T>()
data class Or<T>(val list: List<Parser<T>>) : Parser<T>()
// TODO ask red red wat is dis UwU
data class Repeat<T>(val a: Int, val b: Int, val p: Parser<T>) : Parser<T>()
data class Except<T>(val p: Parser<T>) : Parser<T>()

fun <T> `!`(p: Parser<T>) = Except(p)
infix fun <T> Parser<T>.`=|`(p: RewriteFunc<T>) = Rewrite(this, p)
infix fun <T> Parser<T>.`%`(f: (T) -> (Ast<T>) -> T) = Lens(f, this)
