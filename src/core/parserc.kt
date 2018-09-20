package org.ice1000.ruiko.core

import org.ice1000.ruiko.haskell.*
import org.ice1000.ruiko.lexer.Lexer

typealias RewriteFunc<T> = (State<T>) -> (Ast<T>) -> Ast<T>

data class History<T>(val base: Int, val branch: Int, val ctx: T)

data class LiteralRule(
		val test: (Token) -> Boolean,
		val lexer: Option<() -> Lexer>
)

data class LRInternal(val depth: Int, val name: String)
data class State<T>(
		var lr: MutableSet<LRInternal>,
		var context: T,
		val trace: Trace<Trace<String>>,
		val lang: MutableMap<String, Parser<T>>
) {
	companion object Factory {
		operator fun <T> invoke(top: T): State<T> {
			val trace = Trace<Trace<String>>()
			trace.append(Trace())
			return State(hashSetOf(), top, trace, hashMapOf())
		}

		inline operator fun <reified T> invoke(): State<T> {
			val trace = Trace<Trace<String>>()
			trace.append(Trace())
			return State(hashSetOf(), T::class.java.newInstance(), trace, hashMapOf())
		}

		fun <T, R> leftRecur(self: State<T>, lr: LRInternal, fn: (State<T>) -> R): R {
			self.lr.add(lr)
			println("start lr ${lr.name} at ${self.lr}")
			return fn(self).also {
				self.lr.remove(lr)
				println("end lr ${lr.name} at ${self.lr}")
			}
		}

		fun <T, R> contextualRecovery(self: State<T>, fn: (State<T>) -> R): R {
			val ctx = self.context
			return fn(self).also {
				self.context = ctx
			}
		}
	}

	inline val endIndex get() = trace.endIndex
	inline val maxFetched get() = trace.maxFetched
	inline val current get() = trace[endIndex]
	fun reset(history: History<T>) {
		val (base, branch, ctx) = history
		context = ctx
		trace.reset(base)
		current.reset(branch)
	}

	fun append(string: String) = current.append(string)
	fun commit() = History(trace.commit, current.commit, context)
	operator fun contains(record: String) = record in current
	fun newOne() {
		if (!trace.inc { Trace() }) current.clear()
	}
}

sealed class Parser<out T>
data class Predicate<T>(val pred: (State<T>) -> Boolean) : Parser<T>()
data class Rewrite<T>(val p: Parser<T>, val app: RewriteFunc<T>) : Parser<T>()
data class Literal(val lit: LiteralRule) : Parser<Nothing>()
object Anything : Parser<Nothing>()
data class Lens<T>(val f: (T) -> (Ast<T>) -> T, val p: Parser<T>) : Parser<T>()
data class Named<T>(val name: String, val f: () -> T) : Parser<T>()
data class And<T>(val list: List<Parser<T>>) : Parser<T>() {
	constructor(vararg ps: Parser<T>) : this(ps.toList())
}

data class Or<T>(val list: List<Parser<T>>) : Parser<T>() {
	constructor(vararg ps: Parser<T>) : this(ps.toList())
}

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
infix fun <T> Parser<T>.repeat(atLeast: Int) = repeat(atLeast, -1)
fun <T> Parser<T>.repeat(atLeast: Int, atMost: Int) = Repeat(atLeast, atMost, this)
infix fun <T> Parser<T>.join(p: Parser<T>) = And(this, And(p, this).repeat(0))
fun <T> Parser<T>.item(atLeast: Int, atMost: Int) = Repeat(atLeast, atMost, this)

fun <T> `!`(p: Parser<T>) = Except(p)
infix fun <T> Parser<T>.`=|`(p: RewriteFunc<T>) = Rewrite(this, p)
infix fun <T> Parser<T>.`%`(f: (T) -> (Ast<T>) -> T) = Lens(f, this)
infix fun <T> Parser<T>.`|||`(f: Parser<T>) = otherwise(f)
infix fun <T> Parser<T>.`&&&`(f: Parser<T>) = nextBy(f)
val <T> Parser<T>.`?` get() = toOptional()

sealed class Result<out T>
object Unmatched : Result<Nothing>()
data class Matched<T>(val ast: Ast<T>) : Result<T>()
data class LR<T>(val pObj: String, val stack: (Result<T>) -> Result<T>) : Result<T>()

inline fun <reified T> parse(self: Parser<T>, tokens: List<Token>, state: State<T>) =
		parse(self, tokens, state, T::class.java)

fun <T> parse(self: Parser<T>, tokens: List<Token>, state: State<T>, `class`: Class<out T>): Result<T> = when (self) {
	is Rewrite -> when (val rew = parse(self.p, tokens, state, `class`)) {
		Unmatched -> Unmatched
		is Matched -> Matched(rew.ast `*-` self.app(state))
		is LR -> LR(rew.pObj) { res -> (rew.stack(res) as? Matched)?.let { Matched(self.app(state)(it.ast)) } ?: Unmatched }
	}
	is Predicate ->
		if (self.pred(state)) Matched(Value(`class`.newInstance()))
		else Unmatched
	is Literal ->
		if (tokens.size <= state.endIndex) Unmatched
		else tokens[state.endIndex].takeIf(self.lit.test)?.let {
			state.newOne()
			Matched(Leaf(it))
		} ?: Unmatched
	Anything ->
		if (tokens.size <= state.endIndex) Unmatched
		else tokens[state.endIndex].let {
			state.newOne()
			Matched(Leaf(it))
		}
	is Lens -> when (val lens = parse(self.p, tokens, state, `class`)) {
		Unmatched -> Unmatched
		is Matched -> lens.apply { state.context = ast `*-` self.f(state.context) }
		is LR -> LR(lens.pObj) { ast ->
			when (val lr = ast `*-` lens.stack) {
				Unmatched -> Unmatched
				is Matched -> lr.also { state.context = it.ast `*-` self.f(state.context) }
				else -> unreachable()
			}
		}
	}
	is Named -> TODO()
	is And -> TODO()
	is Or -> TODO()
	is Repeat -> TODO()
	is Except -> TODO()
}
