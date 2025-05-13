
package com.github.cosminchr.osint.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "scan_history")
class ScanHistoryEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne
    @JoinColumn(name = "scan_id", nullable = false)
    var scan: ScanEntity,

    @Enumerated(EnumType.STRING)
    var status: ScanStatus,

    var timestamp: LocalDateTime = LocalDateTime.now(),

    @Column(length = 1024)
    var message: String? = null
)