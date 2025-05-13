package com.github.cosminchr.osint.model

data class AmassParams(
    val verbose: Boolean = true,
    val passive: Boolean = true,
    val timeout: Int = 5, // This timeout is for Amass internal operations, not Docker container
    val minForRecursive: Int = 2,
    val includeUnresolvable: Boolean = false,
    val additionalOptions: String? = null,
)