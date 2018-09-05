package core

val cachingPool = HashMap<String, String>()
fun cache(string: String) = cachingPool[string] ?: string.also { cachingPool += it to it }
