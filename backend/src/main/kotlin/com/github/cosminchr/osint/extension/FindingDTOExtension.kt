package com.github.cosminchr.osint.extenssion

import com.github.cosminchr.osint.dto.FindingDTO
import com.github.cosminchr.osint.model.FindingEntity

fun FindingEntity.toDto(): FindingDTO {
    return FindingDTO(
        id = this.id ?: 0,
        scanId = this.scan?.id ?: 0,
        type = this.type,
        value = this.value,
        ipAddress  = this.ipAddress,
        createdAt = this.createdAt
    )
}