package com.github.cosminchr.osint.repository

import com.github.cosminchr.osint.model.ScanEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ScanRepository : JpaRepository<ScanEntity, Long> {
    fun findByIdAndDeletedFalse(id: Long): Optional<ScanEntity>
    fun findAllByDeletedFalseOrderByStartTimeDesc(): List<ScanEntity>
}