package com.github.cosminchr.osint.model

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Scan(
    val id: Long,
    val domain: String,
    val status: ScanStatus,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime? = null,
    val tool: String,
    val findings: List<String> = emptyList(),
    val summary: String? = null,
    val errorMessage: String? = null
) {
    companion object {
        fun fromEntity(entity: ScanEntity): Scan {
            return Scan(
                id = entity.id ?: 0,
                domain = entity.domain,
                status = ScanStatus.valueOf(entity.status),
                startTime = entity.startTime,
                endTime = entity.endTime,
                tool = entity.scanType,
                findings = entity.findings.map { it.value },
                summary = entity.summary,
                errorMessage = entity.errorMessage
            )
        }
    }
}