/** Helper functions that makes rewriting in Haskell easier */

package org.ice1000.ruiko.haskell

inline fun <reified Obj> id(obj: Obj) = obj
inline infix fun <A, B, C> ((A) -> B).`-*`(crossinline f: (B) -> C) = { a: A -> f(invoke(a)) }
inline infix fun <A, B> A.`*-`(crossinline f: (A) -> B) = f(this)
inline infix fun <A, B> ((A) -> B).`-*`(a: A) = invoke(a)
inline fun <T, reified R> unsafeCoerce(t: T) = t as R
