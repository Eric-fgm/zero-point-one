package com.zeropointone

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.zeropointone.views.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "zeropointone",
    ) {
        App()
    }
}