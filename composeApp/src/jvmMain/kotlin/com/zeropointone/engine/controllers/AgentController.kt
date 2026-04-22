package com.zeropointone.engine.controllers

import com.zeropointone.engine.Agent
import com.zeropointone.engine.Terrain
import com.zeropointone.utils.Position
import kotlinx.coroutines.delay

sealed class AgentController<T : Agent>(protected val terrain: Terrain, protected val agent: T) : Controller() {
    override fun run() {
        this.terrain.place(agent)
        super.run()
    }

    override fun destroy() {
        super.destroy()
        this.terrain.remove(agent)
    }

    class Producer(terrain: Terrain, agent: Agent.Producer) : AgentController<Agent.Producer>(terrain, agent) {
        override suspend fun act() {
            delay(100)
        }
    }

    class Consumer(terrain: Terrain, agent: Agent.Consumer) : AgentController<Agent.Consumer>(terrain, agent) {
        override suspend fun act() {
            this.terrain.move(agent, Position(agent.position.x + 8, agent.position.y))
            delay(1000)
        }
    }
}