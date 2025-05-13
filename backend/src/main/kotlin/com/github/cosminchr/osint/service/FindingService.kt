package com.github.cosminchr.osint.service

import com.github.cosminchr.osint.dto.FindingDTO
import com.github.cosminchr.osint.extenssion.toDto
import com.github.cosminchr.osint.repository.FindingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FindingService(private val findingRepository: FindingRepository) {

    @Transactional(readOnly = true)
    fun getFindingsByScanId(scanId: Long): List<FindingDTO> {
        return findingRepository.findByScanId(scanId).map { it.toDto() }
    }

    @Transactional(readOnly = true)
    fun getFindingsByScanIdAndType(scanId: Long, type: String): List<FindingDTO> {
        return findingRepository.findByScanIdAndType(scanId, type).map { it.toDto() }
    }

    @Transactional(readOnly = true)
    fun countFindingsByScanId(scanId: Long): Map<String, Long> {
        val totalCount = findingRepository.countByScanId(scanId)
        val subdomainCount = findingRepository.countByScanIdAndType(scanId, "subdomain")
        val ipAddressCount = findingRepository.countByScanIdAndType(scanId, "ip_address")
        val nameserverCount = findingRepository.countByScanIdAndType(scanId, "nameserver")

        return mapOf(
            "total" to totalCount,
            "subdomain" to subdomainCount,
            "ip_address" to ipAddressCount,
            "nameserver" to nameserverCount
        )
    }
}