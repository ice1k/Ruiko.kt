package org.ice1000.ruiko.core

/**
 * Created by redy red, don't know why
 * @param T
 * @property records [MutableList]
 * @property commit [Int]
 * @property endIndex [Int]
 * @property maxFetched [Int]
 */
class Trace<T> {
	private val records: MutableList<T> = mutableListOf()
	var commit: Int = 0
		private set

	val endIndex get() = commit - 1
	val maxFetched get() = records.size

	init {
		clear()
	}

	fun clear() = reset(0)
	fun size() = commit
	operator fun get(i: Int) = if (i >= commit) throw IndexOutOfBoundsException() else records[i]
	fun reset(history: Int) {
		commit = history
	}

	fun append(e: T) {
		val sLen = commit
		if (maxFetched == sLen) {
			records.add(e)
			commit++
			return
		}
		commit++
		records[sLen] = e
	}

	fun inc(supplier: () -> T): Boolean {
		val sLen = commit
		if (maxFetched == sLen) {
			records.add(supplier())
			commit++
			return true
		}
		commit++
		return false
	}

	operator fun contains(e: T): Boolean {
		for (i in 0 until commit) {
			if (records[i] == e) return true
		}
		return false
	}
}


