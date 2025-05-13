package com.github.cosminchr.osint.model

data class Finding(
    val value: String,
    val type: String,
    val source: String = "amass",
    val ipAddress: String? = null,
    val cnameValue: String? = null
)