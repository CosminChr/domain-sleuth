package com.github.cosminchr.osint.controller

import com.github.cosminchr.osint.dto.ScanHistoryDTO
import com.github.cosminchr.osint.service.ScanHistoryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/scans")
class ScanHistoryController(
    private val scanHistoryService: ScanHistoryService
) {
    @GetMapping("/{id}/history")
    fun getScanHistory(@PathVariable id: Long): ResponseEntity<List<ScanHistoryDTO>> {
        return try {
            val history = scanHistoryService.getScanHistory(id)
                .map { entity ->
                    ScanHistoryDTO(
                        id = entity.id ?: 0,
                        scanId = entity.scan.id ?: 0,
                        status = entity.status.name,
                        timestamp = entity.timestamp,
                        message = entity.message
                    )
                }
            ResponseEntity.ok(history)
        } catch (_: Exception) {
            ResponseEntity.badRequest().build()
        }
    }
}