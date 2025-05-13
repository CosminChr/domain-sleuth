package com.github.cosminchr.osint.repository

import com.github.cosminchr.osint.model.FindingEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FindingRepository : JpaRepository<FindingEntity, Long> {
    fun findByScanId(scanId: Long): List<FindingEntity>
    fun findByScanIdAndType(scanId: Long, type: String): List<FindingEntity>
    fun countByScanId(scanId: Long): Long
    fun countByScanIdAndType(scanId: Long, type: String): Long
}