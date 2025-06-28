package com.embabel.example.travel.agent

import com.embabel.common.core.types.Timestamped
import java.time.Instant

data class ReportRequest(
    val topic: String,
    val words: Int,
    override val timestamp: Instant = Instant.now(),
) : Timestamped

