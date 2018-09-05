@file:JvmName("Jojo! This is the last of my caching pool! Take it from MEEEEEEE!!")
package org.ice1000.ruiko.core

internal val cachingPool = HashMap<String, String>()
internal fun cache(string: String) = cachingPool[string] ?: string.also { cachingPool += it to it }
