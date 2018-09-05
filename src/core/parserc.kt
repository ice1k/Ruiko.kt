package org.ice1000.ruiko.core

import org.ice1000.ruiko.haskell.Maybe
import org.ice1000.ruiko.haskell.`*-`
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
data class And<T>(val list: List<Parser<T>>) : Parser<T>() { constructor(vararg ps: Parser<T>) : this(ps.toList()) }
data class Or<T>(val list: List<Parser<T>>) : Parser<T>() { constructor(vararg ps: Parser<T>) : this(ps.toList()) }
data class Repeat<T>(val atLeast: Int, val atMost: Int, val p: Parser<T>) : Parser<T>()
data class Except<T>(val p: Parser<T>) : Parser<T>()

infix fun <T> Parser<T>.otherwise(p: Parser<T>) = when (this) {
	is Or -> if (p is Or) list + p.list else list + p
	else -> if (p is Or) listOf(this) + p.list else listOf(this, p)
} `*-` ::Or

infix fun <T> Parser<T>.nextBy(p: Parser<T>) = when (this) {
	is And -> if (p is And) list + p.list else list + p
	else -> if (p is And) listOf(this) + p.list else listOf(this, p)
} `*-` ::And

fun <T> optional(p: Parser<T>) = p.toOptional()
fun <T> Parser<T>.toOptional() = repeat(0, 1)
fun <T> Parser<T>.repeat(atLeast: Int) = repeat(atLeast, -1)
fun <T> Parser<T>.repeat(atLeast: Int, atMost: Int) = Repeat(atLeast, atMost, this)
fun <T> Parser<T>.join(p: Parser<T>) = And(this, And(p, this).repeat(0))

fun <T> `!`(p: Parser<T>) = Except(p)
infix fun <T> Parser<T>.`=|`(p: RewriteFunc<T>) = Rewrite(this, p)
infix fun <T> Parser<T>.`%`(f: (T) -> (Ast<T>) -> T) = Lens(f, this)
infix fun <T> Parser<T>.`|||`(f: Parser<T>) = otherwise(f)
infix fun <T> Parser<T>.`&&&`(f: Parser<T>) = nextBy(f)
val <T> Parser<T>.`?` get() = toOptional()

