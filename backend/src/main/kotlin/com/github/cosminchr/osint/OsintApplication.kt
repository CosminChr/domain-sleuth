package com.github.cosminchr.osint

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
class OsintApplication

fun main(args: Array<String>) {
    runApplication<OsintApplication>(*args)
}
