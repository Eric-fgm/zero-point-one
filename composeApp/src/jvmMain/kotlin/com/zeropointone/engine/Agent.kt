package com.zeropointone.engine

import com.zeropointone.enums.ConsumerLevel
import com.zeropointone.utils.Position
import java.util.UUID

sealed class Agent(protected val id: UUID = UUID.randomUUID(), var position: Position) {
    class Producer(position: Position): Agent(position=position) {}

    class Consumer(position: Position, level: ConsumerLevel) : Agent(position=position) {}
}