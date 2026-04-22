package com.zeropointone.engine.controllers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class Controller {
    private var job: Job? = null

    open fun run() {
        this.job = CoroutineScope(Dispatchers.Default).launch {
            while (true) { act() }
        }
    }

    open fun destroy() {
        this.job?.cancel()
    }

    protected abstract suspend fun act()
}