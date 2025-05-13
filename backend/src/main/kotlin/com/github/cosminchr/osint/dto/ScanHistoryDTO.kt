package com.github.cosminchr.osint.dto

import java.time.LocalDateTime

data class ScanHistoryDTO(
    val id: Long,
    val scanId: Long,
    val status: String,
    val timestamp: LocalDateTime,
    val message: String?
)