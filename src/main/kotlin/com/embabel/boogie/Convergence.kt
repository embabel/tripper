package com.embabel.boogie

/**
 * Convergence between a resolution, with an ultimate convergence target.
 */
data class Convergence<R, T>(
    val resolution: R,
    val convergenceTarget: T?
)