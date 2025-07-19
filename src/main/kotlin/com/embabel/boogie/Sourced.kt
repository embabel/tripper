package com.embabel.boogie

import com.embabel.agent.rag.Retrievable

interface Sourced {
    val basis: Retrievable
}