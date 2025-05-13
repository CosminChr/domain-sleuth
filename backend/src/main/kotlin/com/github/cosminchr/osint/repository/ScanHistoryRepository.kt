package com.github.cosminchr.osint.repository

import com.github.cosminchr.osint.model.ScanHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ScanHistoryRepository : JpaRepository<ScanHistoryEntity, Long> {
    fun findByScanIdOrderByTimestampDesc(scanId: Long): List<ScanHistoryEntity>
}