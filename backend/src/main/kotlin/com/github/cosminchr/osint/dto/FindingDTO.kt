package com.github.cosminchr.osint.dto

import java.time.LocalDateTime

data class FindingDTO(
    val id: Long?,
    val scanId: Long?,
    val type: String,
    val value: String,
    val ipAddress: String?,
    val cnameValue: String?,
    val createdAt: LocalDateTime?
)