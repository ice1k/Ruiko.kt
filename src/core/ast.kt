package core

data class Token(
		val fileName: String,
		val lineN: Int,
		val columnN: Int,
		val offset: Int,
		val name: String,
		val value: String
)

sealed class Ast<out T>
data class Value<T>(val obj: T) : Ast<T>()
data class MExpr<T>(val string: String, val ast: Ast<T>) : Ast<T>()
data class Nested<T>(val list: List<Ast<T>>) : Ast<T>()
data class Leaf(val token: Token) : Ast<Nothing>()

fun <T> mergeNested(nested: MutableList<Ast<T>>) = fun (ast: Ast<T>) = when (ast) {
	is Nested -> nested.addAll(ast.list)
	else -> nested.add(ast)
}
