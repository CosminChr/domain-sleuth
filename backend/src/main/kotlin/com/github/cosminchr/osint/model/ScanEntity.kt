package com.github.cosminchr.osint.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "scan")
data class ScanEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val domain: String,

    @Column(name = "scan_type", nullable = false)
    val scanType: String,

    @Column(nullable = false)
    var status: String,

    @Column(name = "start_time", nullable = false)
    val startTime: LocalDateTime,

    @Column(name = "end_time")
    var endTime: LocalDateTime? = null,

    @Column(columnDefinition = "TEXT")
    var summary: String? = null,

    @Column(name = "error_message")
    var errorMessage: String? = null,

    @Column(nullable = false)
    var deleted: Boolean = false,

    @OneToMany(
        mappedBy = "scan",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    val findings: MutableSet<FindingEntity> = mutableSetOf(),

    @OneToMany(
        mappedBy = "scan",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    val history: MutableSet<ScanHistoryEntity> = mutableSetOf()
)