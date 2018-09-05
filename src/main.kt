package org.ice1000.ruiko

import org.ice1000.jimgui.cpp.DeallocatableObjectManager
import org.ice1000.jimgui.dsl.JImGuiContext
import org.ice1000.jimgui.util.JniLoader

const val WIDTH = 800
const val HEIGHT = 600
const val TITLE = "Lancer"

fun main(vararg args: String) {
}

fun gui() {
	JniLoader.load()
	DeallocatableObjectManager().use { _ ->
		JImGuiContext(WIDTH, HEIGHT, TITLE).use { gui ->
			while (!gui.windowShouldClose()) {
			}
		}
	}
}

