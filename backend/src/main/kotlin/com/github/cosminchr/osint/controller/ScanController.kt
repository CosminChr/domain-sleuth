package com.github.cosminchr.osint.controller

import com.github.cosminchr.osint.dto.FindingDTO
import com.github.cosminchr.osint.model.Scan
import com.github.cosminchr.osint.service.FindingService
import com.github.cosminchr.osint.service.ScanService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/scans")
class ScanController(
    private val scanService: ScanService,
    private val findingService: FindingService
) {

    @GetMapping
    fun getAllScans(): ResponseEntity<List<Scan>> {
        val scans = scanService.getAllScans()
        return ResponseEntity.ok(scans)
    }

    @GetMapping("/{id}")
    fun getScan(@PathVariable id: Long): ResponseEntity<Scan> {
        return try {
            val scan = scanService.getScan(id)
            ResponseEntity.ok(scan)
        } catch (_: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping
    fun createScan(@RequestBody request: CreateScanRequest): ResponseEntity<Scan> {
        return try {
            val scan = scanService.createScan(
                domain = request.domain,
                config = request.config ?: emptyMap(),
                tool = request.tool ?: "amass"
            )
            ResponseEntity.ok(scan)
        } catch (_: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteScan(@PathVariable id: Long): ResponseEntity<Void> {
        return try {
            scanService.deleteScan(id)
            ResponseEntity.noContent().build()
        } catch (_: NoSuchElementException) {
            ResponseEntity.notFound().build()
        } catch (_: Exception) {
            ResponseEntity.internalServerError().build()
        }
    }

    @GetMapping("/{id}/findings")
    fun getScanFindings(@PathVariable id: Long): ResponseEntity<List<FindingDTO>> {
        return try {
            val findings = findingService.getFindingsByScanId(id)
            ResponseEntity.ok(findings)
        } catch (_: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/{id}/findings/count")
    fun countScanFindings(@PathVariable id: Long): ResponseEntity<Map<String, Long>> {
        return try {
            val counts = findingService.countFindingsByScanId(id)
            ResponseEntity.ok(counts)
        } catch (_: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/{id}/findings/{type}")
    fun getScanFindingsByType(
        @PathVariable id: Long,
        @PathVariable type: String
    ): ResponseEntity<List<FindingDTO>> {
        return try {
            val findings = findingService.getFindingsByScanIdAndType(id, type)
            ResponseEntity.ok(findings)
        } catch (_: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }
    }
}

data class CreateScanRequest(
    val domain: String,
    val config: Map<String, Any>? = null,
    val tool: String? = null
)