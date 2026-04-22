package com.zeropointone.engine

import com.zeropointone.engine.controllers.AgentController
import com.zeropointone.engine.controllers.Controller
import com.zeropointone.enums.ConsumerLevel
import com.zeropointone.utils.Position

class Engine(val terrain: Terrain) {
    private val controllers: MutableList<Controller> = mutableListOf()

    fun run() {
        if (controllers.isNotEmpty()) return

        this.spawn()
        this.controllers.forEach(Controller::run)
    }

    fun destroy() {
        this.controllers.forEach(Controller::destroy)
        this.controllers.clear()
    }

    private fun spawn() {
        repeat(50) {
            val agent = Agent.Producer(position=Position.generateRandom(0..terrain.pixelWidth, 0..terrain.pixelHeight))
            controllers.add(AgentController.Producer(this.terrain, agent))
        }

        repeat(75) {
            val agent = Agent.Consumer(
                position=Position.generateRandom(0..terrain.pixelWidth, 0..terrain.pixelHeight),
                level= ConsumerLevel.Primary
            )
            controllers.add(AgentController.Consumer(this.terrain, agent))
        }
    }
}