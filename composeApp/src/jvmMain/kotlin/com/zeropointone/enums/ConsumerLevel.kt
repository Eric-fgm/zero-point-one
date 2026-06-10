package com.zeropointone.enums

/**
 * Consumer trophic levels. Producers are trophic level 1 (not a consumer),
 * so primary consumers (herbivores) start at level 2.
 */
enum class ConsumerLevel(val trophicLevel: Int) {
    Primary(2),
    Secondary(3),
    Tertiary(4),
    Quaternary(5);

    /** Strict stratification: a consumer of level n only eats level n-1. */
    val preyLevel: Int get() = trophicLevel - 1

    companion object {
        fun ofTrophicLevel(level: Int): ConsumerLevel? =
            entries.firstOrNull { it.trophicLevel == level }
    }
}
