package com.github.cosminchr.osint.service

import com.github.cosminchr.osint.model.*
import com.github.cosminchr.osint.repository.ScanRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class ScanService @Autowired constructor(
    private val scanRepository: ScanRepository,
    private val scanHistoryService: ScanHistoryService,
    private val osintScannerService: OsintScannerService
) {
    private val logger = LoggerFactory.getLogger(ScanService::class.java)

    fun getAllScans(): List<Scan> {
        return scanRepository.findAllByDeletedFalseOrderByStartTimeDesc().map { Scan.fromEntity(it) }
    }

    fun getScan(id: Long): Scan {
        val entity = scanRepository.findByIdAndDeletedFalse(id).orElseThrow {
            NoSuchElementException("Scan not found with id: $id")
        }
        return Scan.fromEntity(entity)
    }

    @Transactional
    fun createScan(domain: String, config: Map<String, Any> = emptyMap(), tool: String = "amass"): Scan {
        // Validate domain
        if (domain.isBlank()) {
            throw IllegalArgumentException("Domain cannot be empty")
        }

        // Create scan entity
        val scanEntity = ScanEntity(
            domain = domain,
            scanType = tool,
            status = ScanStatus.PENDING.name,
            startTime = LocalDateTime.now()
        )

        // Format config as scan summary
        val configStr = if (config.isNotEmpty()) {
            config.entries.joinToString(", ") { "${it.key}=${it.value}" }
        } else {
            "Default scan configuration"
        }
        scanEntity.summary = configStr

        logger.info("Creating scan for domain: $domain with config: $configStr")

        // Save the scan
        val savedScanEntity = scanRepository.save(scanEntity)

        // Create initial history entry
        scanHistoryService.addHistoryEntry(
            savedScanEntity,
            ScanStatus.PENDING,
            "Scan created for domain: $domain"
        )

        // Run scan asynchronously (don't block the API response)
        val scan = Scan.fromEntity(savedScanEntity)
        runScanAsync(savedScanEntity, scan)

        return scan
    }

    private fun runScanAsync(scanEntity: ScanEntity, scan: Scan) {
        // Use a thread pool to avoid blocking Spring's @Async thread pool
        Thread {
            try {
                // Check if the scan still exists and is not deleted
                val freshEntity = scanRepository.findByIdAndDeletedFalse(scanEntity.id!!)
                if (freshEntity.isEmpty) {
                    logger.info("Scan ${scanEntity.id} has been deleted or doesn't exist, aborting processing")
                    return@Thread
                }

                // Update status to IN_PROGRESS
                scanEntity.status = ScanStatus.IN_PROGRESS.name
                scanRepository.save(scanEntity)

                scanHistoryService.addHistoryEntry(
                    scanEntity,
                    ScanStatus.IN_PROGRESS,
                    "Scan started for domain: ${scanEntity.domain}"
                )

                // Run the appropriate scan based on the tool
                // Important: No longer using @Async method to avoid the return type issue
                val findings = when (scanEntity.scanType.lowercase()) {
                    "amass" -> osintScannerService.runAmassScanAsync(scanEntity)
                    else -> throw IllegalArgumentException("Unsupported tool: ${scanEntity.scanType}")
                }

                // Check again if the scan still exists and is not deleted
                val stillExists = scanRepository.findByIdAndDeletedFalse(scanEntity.id!!)
                if (stillExists.isEmpty) {
                    logger.info("Scan ${scanEntity.id} has been deleted during processing, aborting update")
                    return@Thread
                }

                // Update scan with findings
                val findingEntities = findings.map { finding ->
                    FindingEntity(
                        scan = scanEntity,
                        type = "result",
                        value = finding
                    )
                }

                // We need to clear and add instead of reassigning
                scanEntity.findings.clear()
                scanEntity.findings.addAll(findingEntities)

                scanEntity.status = ScanStatus.COMPLETED.name
                scanEntity.endTime = LocalDateTime.now()
                scanRepository.save(scanEntity)

                scanHistoryService.addHistoryEntry(
                    scanEntity,
                    ScanStatus.COMPLETED,
                    "Scan completed for domain: ${scanEntity.domain} with ${findings.size} findings"
                )

                logger.info("Scan completed for domain: ${scanEntity.domain}")
            } catch (e: Exception) {
                logger.error("Error running scan for domain ${scanEntity.domain}: ${e.message}", e)

                try {
                    // Check if the scan still exists and is not deleted before updating it as failed
                    val stillExists = scanRepository.findByIdAndDeletedFalse(scanEntity.id!!)
                    if (stillExists.isPresent) {
                        // Mark as failed
                        scanEntity.status = ScanStatus.FAILED.name
                        scanEntity.errorMessage = e.message
                        scanEntity.endTime = LocalDateTime.now()
                        scanRepository.save(scanEntity)

                        scanHistoryService.addHistoryEntry(
                            scanEntity,
                            ScanStatus.FAILED,
                            "Scan failed: ${e.message}"
                        )
                    } else {
                        logger.info("Scan ${scanEntity.id} has been deleted during processing, not marking as failed")
                    }
                } catch (innerEx: Exception) {
                    logger.error("Error updating scan status to FAILED: ${innerEx.message}", innerEx)
                }
            }
        }.start()
    }

    @Transactional
    fun deleteScan(id: Long) {
        // Fetch the scan entity first instead of just checking existence
        val scanEntity = scanRepository.findById(id).orElseThrow {
            NoSuchElementException("Scan not found with id: $id")
        }

        // Log deletion
        logger.info("Marking scan as deleted with ID: $id, domain: ${scanEntity.domain}")

        try {
            // Perform logical deletion instead of physical deletion
            scanEntity.deleted = true
            scanRepository.save(scanEntity)
            logger.info("Successfully marked scan as deleted with ID: $id")
        } catch (e: Exception) {
            logger.error("Error while marking scan as deleted with ID: $id", e)
            throw e
        }
    }
}