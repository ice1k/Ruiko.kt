package org.ice1000.ruiko.lexer

import org.ice1000.ruiko.core.Token
import org.ice1000.ruiko.core.cache
import org.ice1000.ruiko.haskell.*
import kotlin.coroutines.experimental.buildSequence

sealed class LexerFactor
data class RegexFactor(val regex: Regex) : LexerFactor()
data class StringFactor(val string: Sequence<String>) : LexerFactor()

data class StringView(val ref: String, val offset: Int) : CharSequence {
	override operator fun get(index: Int) = ref[offset + index]
	override val length get() = ref.length - offset
	override fun subSequence(startIndex: Int, endIndex: Int) = ref.subSequence(startIndex + offset, endIndex + offset)
}

fun lexerFactorMatch(stringView: StringView) = fun(factor: LexerFactor) = when (factor) {
	is RegexFactor -> factor.regex.matchEntire(stringView)?.value
	is StringFactor -> factor.string.firstOrNull { stringView.startsWith(it) }
}.toMaybe()

typealias LexerMatcher = (StringView) -> Maybe<String>
typealias LexerTable = Sequence<Lexer>
typealias CastMap = MutableMap<String, String>

data class Lexer(val name: String, val factor: LexerFactor)
data class Source(val fileName: String, val text: String)

fun lex(castMap: Maybe<CastMap>) = fun(lexerTable: LexerTable) = fun(src: Source) {
	val view = StringView(src.text, 0)
	val n = src.text.length
	var lineN = 0
	var columnN = 0
	val fileName = src.fileName
	fun loop(view: StringView) = buildSequence {
		if (view.offset >= n) return@buildSequence
		val match = lexerFactorMatch(view)
		lexerTable
				.map { (n, f) -> match(f).map { n to it } }
				.firstJust()
				.getOr {
					yield(Left("Unknown string head: `${view.ref.take(15)}` at ($lineN, $columnN) in $fileName"))
					return@buildSequence
				}
				.let {
					val (name, word) = it
					val (tokenName, tokenWord) = castMap.map {
						cache(name) to (it[word]?.let { cache(word) } ?: word)
					}.getOr(it)
					yield(Right(Token(
							name = tokenName,
							value = tokenWord,
							fileName = fileName,
							columnN = columnN,
							offset = view.offset,
							lineN = lineN
					)))
					val wordLen = word.length
					val eolCount = word.count('\n'::equals)
					if (eolCount == 0) columnN += wordLen
					else {
						lineN += eolCount
						columnN += wordLen - word.indexOfLast('\n'::equals) - 1
					}
					yieldAll(loop(StringView(view.ref, view.offset + wordLen)))
				}
	}
	loop(view)
}
