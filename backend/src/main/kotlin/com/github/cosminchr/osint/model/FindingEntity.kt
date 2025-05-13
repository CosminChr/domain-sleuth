package com.github.cosminchr.osint.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "finding", indexes = [
    Index(name = "idx_finding_scan_id", columnList = "scan_id"),
    Index(name = "idx_finding_type", columnList = "type")
])
data class FindingEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_id")
    var scan: ScanEntity? = null,

    @Column(nullable = false)
    val type: String,

    @Column(columnDefinition = "TEXT", nullable = false)
    val value: String,

    @Column // Allow null if IP address might not always be present or applicable
    val ipAddress: String? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FindingEntity

        if (id != other.id) return false
        if (type != other.type) return false
        if (value != other.value) return false
        if (ipAddress != other.ipAddress) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + type.hashCode()
        result = 31 * result + value.hashCode()
        result = 31 * result + (ipAddress?.hashCode() ?: 0)
        return result
    }

    // Avoid circular references in toString
    override fun toString(): String {
        return "FindingEntity(id=$id, scanId=${scan?.id}, type='$type', value='$value', ipAddress='$ipAddress', createdAt=$createdAt)"
    }
}