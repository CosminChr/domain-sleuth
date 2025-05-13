package com.github.cosminchr.osint.service

import com.github.cosminchr.osint.model.ScanEntity
import com.github.cosminchr.osint.model.ScanHistoryEntity
import com.github.cosminchr.osint.model.ScanStatus
import com.github.cosminchr.osint.repository.ScanHistoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ScanHistoryService(
    private val scanHistoryRepository: ScanHistoryRepository
) {
    @Transactional
    fun addHistoryEntry(
        scan: ScanEntity,
        status: ScanStatus,
        message: String? = null
    ): ScanHistoryEntity {
        val historyEntry = ScanHistoryEntity(
            scan = scan,
            status = status,
            timestamp = LocalDateTime.now(),
            message = message
        )

        return scanHistoryRepository.save(historyEntry)
    }

    fun getScanHistory(scanId: Long): List<ScanHistoryEntity> {
        return scanHistoryRepository.findByScanIdOrderByTimestampDesc(scanId)
    }
}