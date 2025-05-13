package com.github.cosminchr.osint.service

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.WaitContainerResultCallback
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PullResponseItem
import com.github.dockerjava.api.async.ResultCallback
import com.github.cosminchr.osint.model.FindingEntity
import com.github.cosminchr.osint.model.ScanEntity
import com.github.cosminchr.osint.repository.FindingRepository
import com.github.cosminchr.osint.model.AmassParams
import com.github.cosminchr.osint.model.Finding
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.exception.NotModifiedException

@Service
class OsintScannerService @Autowired constructor(
    private val dockerClient: DockerClient,
    private val findingRepository: FindingRepository,
    @Value("\${app.docker.amass-image:caffix/amass}")
    private val amassImage: String,
    @Value("\${app.docker.container-timeout-minutes:30}")
    private val containerTimeoutMinutes: Long
) {
    private val logger = LoggerFactory.getLogger(OsintScannerService::class.java)
    private val executor: ExecutorService = Executors.newCachedThreadPool()

    init {
        try {
            dockerClient.pingCmd().exec()
            logger.info("Successfully connected to Docker daemon")
        } catch (e: Exception) {
            logger.error("Failed to connect to Docker daemon: ${e.message}", e)
        }
        pullImage(amassImage)
    }

    private fun pullImage(image: String) {
        try {
            logger.info("Checking for Docker image: $image")
            try {
                dockerClient.inspectImageCmd(image).exec()
                logger.info("Image $image already exists locally.")
                return
            } catch (e: NotFoundException) {
                logger.info("Image $image not found locally, proceeding with pull.")
            }

            val result = CountDownLatch(1)
            var errorOccurred = false
            dockerClient.pullImageCmd(image)
                .exec(object : ResultCallback.Adapter<PullResponseItem>() {
                    override fun onNext(item: PullResponseItem?) {
                        logger.debug("Pulling image $image: ${item?.status} ${item?.progressDetail?.current ?: ""}/${item?.progressDetail?.total ?: ""}")
                    }
                    override fun onComplete() {
                        logger.info("Image pull complete: $image")
                        result.countDown()
                        super.onComplete()
                    }
                    override fun onError(throwable: Throwable) {
                        logger.error("Error pulling image $image: ${throwable.message}", throwable)
                        errorOccurred = true
                        result.countDown()
                        super.onError(throwable)
                    }
                })

            if (!result.await(5, TimeUnit.MINUTES)) {
                logger.warn("Image pull for $image timed out after 5 minutes")
            } else if (errorOccurred) {
                logger.error("Image pull for $image failed.")
            }

        } catch (e: Exception) {
            logger.error("Failed to pull Docker image $image: ${e.message}", e)
        }
    }

    fun runAmassScanAsync(scan: ScanEntity): List<String> {
        return runAmassScan(scan.domain, scan, getAmassParams(scan))
    }

    @Transactional
    fun runAmassScan(
        domain: String,
        scanEntity: ScanEntity? = null,
        params: AmassParams = AmassParams()
    ): List<String> {
        val hostname = "amass-${System.currentTimeMillis()}"
        val outputDir = Paths.get(System.getProperty("java.io.tmpdir"), "amass-output", domain, hostname).toString()
        val outputPath = Paths.get(outputDir, "${domain.replace("[^a-zA-Z0-9.-]", "_")}.txt")


        Files.createDirectories(Paths.get(outputDir))
        logger.info("Created output directory: $outputDir for Amass scan on $domain")

        val command = buildAmassCommand(domain, params, outputPath.toString())
        logger.info("Running Amass command for $domain: '$command'")

        var containerId: String? = null
        val allLogs = mutableListOf<String>()
        var logCollector: Future<*>? = null

        try {
            val containerCreationCmd = dockerClient.createContainerCmd(amassImage)
                .withName(hostname)
                .withEntrypoint("sh", "-c", command)
                .withHostConfig(
                    HostConfig.newHostConfig()
                        .withBinds(
                            com.github.dockerjava.api.model.Bind(
                                outputDir,
                                com.github.dockerjava.api.model.Volume("/output")
                            )
                        )
                )
            val container = containerCreationCmd.exec()
            containerId = container.id
            logger.info("Created container ${container.id} for $domain with name $hostname")

            logCollector = streamContainerLogs(container.id) { logLine ->
                allLogs.add(logLine)
            }

            dockerClient.startContainerCmd(container.id).exec()
            logger.info("Started container ${container.id} for $domain.")

            val waitCallback = WaitContainerResultCallback()
            dockerClient.waitContainerCmd(container.id).exec(waitCallback)

            logger.info("Waiting for container ${container.id} to complete (timeout: $containerTimeoutMinutes minutes)...")
            val completedNormally = waitCallback.awaitCompletion(containerTimeoutMinutes, TimeUnit.MINUTES)
            var exitCode: Long = -1L

            if (completedNormally) {
                try {
                    exitCode = waitCallback.awaitStatusCode()?.toLong() ?: -1L
                    logger.info("Container ${container.id} completed normally with exit code $exitCode.")
                } catch (e: Exception) {
                    logger.warn("Error getting status code for completed container ${container.id}: ${e.message}", e)
                }
            } else {
                logger.warn("Container ${container.id} timed out after $containerTimeoutMinutes minutes. Attempting to stop and process partial results.")
                try {
                    dockerClient.stopContainerCmd(container.id).withTimeout(30).exec()
                    logger.info("Attempted to stop container ${container.id} due to timeout.")
                } catch (stopEx: NotModifiedException) {
                    logger.info("Container ${container.id} was already stopped or not running.")
                } catch (stopEx: Exception) {
                    logger.warn("Could not stop container ${container.id} after timeout: ${stopEx.message}", stopEx)
                }
            }

            logCollector?.cancel(true)
            logger.debug("Log collector cancelled for container ${container.id}.")

            val directLogs = getContainerLogs(container.id)
            logger.info("Collected ${directLogs.size} direct logs for container ${container.id}.")

            val findings = parseFindings(allLogs, directLogs, domain)
            logger.info("Parsed ${findings.size} findings for domain $domain.")

            if (scanEntity != null) {
                val findingEntities = findings.map { finding ->
                    FindingEntity(
                        scan = scanEntity,
                        type = finding.type,
                        value = finding.value,
                        ipAddress = finding.ipAddress,
                        cnameValue = finding.cnameValue
                    )
                }
                if (findingEntities.isNotEmpty()) {
                    logger.info("Saving ${findingEntities.size} findings to database for scan ${scanEntity.id}.")
                    saveFindingsToDatabase(scanEntity, findingEntities)
                } else {
                    logger.info("No new findings to save for scan ${scanEntity.id}.")
                }
            }

            val summaryMessages = mutableListOf<String>()
            if (!completedNormally) {
                summaryMessages.add("Warning: Scan timed out. Partial results are shown below.")
                if (findings.isNotEmpty()) {
                    summaryMessages.addAll(generateFindingSummaries(findings))
                } else {
                    summaryMessages.add("No findings were collected before timeout.")
                }
                summaryMessages.add("Exit code: N/A (timed out)")
            } else if (exitCode != 0L && findings.isEmpty()) {
                summaryMessages.add("Error: Scan completed with exit code $exitCode and no findings.")
                val errorFindings = findings.filter { it.type == "error" }
                if (errorFindings.isNotEmpty()) {
                    summaryMessages.add("Scan reported the following errors:")
                    errorFindings.take(3).forEach { summaryMessages.add("- ${it.value}") }
                } else if (directLogs.any { it.contains("level=error", ignoreCase = true) || it.contains("level=fatal", ignoreCase = true) }) {
                    summaryMessages.add("Check container logs for detailed error information.")
                }
            } else if (findings.isEmpty() && exitCode == 0L) {
                summaryMessages.add("Scan completed successfully with no findings.")
                summaryMessages.add("Exit code: $exitCode")
            } else {
                if (exitCode == 0L) {
                    summaryMessages.add("Scan completed successfully.")
                } else {
                    summaryMessages.add("Scan completed with exit code $exitCode (see findings below).")
                }
                summaryMessages.addAll(generateFindingSummaries(findings))
            }
            return summaryMessages

        } catch (e: Exception) {
            logger.error("Critical error during Amass scan for $domain (containerId: $containerId): ${e.message}", e)
            logCollector?.cancel(true)
            return listOf("Error running scan for $domain: ${e.message}")
        } finally {
            containerId?.let { cId ->
                try {
                    logger.info("Attempting to remove container $cId for $domain.")
                    dockerClient.removeContainerCmd(cId).withForce(true).exec()
                    logger.info("Successfully removed container $cId for $domain.")
                } catch (e: NotFoundException) {
                    logger.warn("Container $cId not found for removal, possibly already removed.")
                } catch (e: Exception) {
                    logger.warn("Failed to remove container $cId for $domain: ${e.message}", e)
                }
                try {
                    Paths.get(outputDir).toFile().deleteRecursively()
                    logger.info("Cleaned up output directory: $outputDir")
                } catch (e: Exception) {
                    logger.warn("Failed to clean up output directory $outputDir: ${e.message}", e)
                }
            }
        }
    }

    private fun buildAmassCommand(domain: String, params: AmassParams, outputPathInContainer: String): String {
        val commandBuilder = StringBuilder("amass enum")
        commandBuilder.append(" -d $domain")
        val safeFileName = Paths.get(outputPathInContainer).fileName.toString().replace("[^a-zA-Z0-9._-]", "_")
        commandBuilder.append(" -o /output/$safeFileName")

        if (params.passive) commandBuilder.append(" -passive")
        if (params.verbose) commandBuilder.append(" -v")
        if (params.timeout > 0) commandBuilder.append(" -timeout ${params.timeout}")
        if (params.minForRecursive > 0) commandBuilder.append(" -min-for-recursive ${params.minForRecursive}")
        if (params.includeUnresolvable) commandBuilder.append(" -include-unresolvable")

        params.additionalOptions?.takeIf { it.isNotBlank() }?.let { options ->
            commandBuilder.append(" $options")
        }
        return commandBuilder.toString()
    }

    private fun getAmassParams(scan: ScanEntity): AmassParams {
        val logger = LoggerFactory.getLogger(javaClass)

        // Parse the scan.summary field which contains configuration in "key=value, key2=value2" format
        val configMap = mutableMapOf<String, Any>()

        scan.summary?.let { summary ->
            if (summary.isNotBlank() && summary != "Default scan configuration") {
                try {
                    summary.split(",").forEach { pair ->
                        val keyValue = pair.trim().split("=", limit = 2)
                        if (keyValue.size == 2) {
                            val key = keyValue[0].trim()
                            val value = keyValue[1].trim()

                            // Convert string values to appropriate types
                            val parsedValue: Any = when {
                                value.equals("true", ignoreCase = true) -> true
                                value.equals("false", ignoreCase = true) -> false
                                value.toIntOrNull() != null -> value.toInt()
                                else -> value
                            }
                            configMap[key] = parsedValue
                        }
                    }
                    logger.debug("Parsed scan configuration from summary for scan ID ${scan.id}: $configMap")
                } catch (e: Exception) {
                    logger.warn("Failed to parse configuration from summary for scan ID ${scan.id}: ${e.message}", e)
                }
            }
        }

        // If the configuration map is empty, use default values
        if (configMap.isEmpty()) {
            logger.info("Using default Amass parameters for scan ID: ${scan.id ?: "N/A"}")
        }

        // Map includeNS from frontend to includeUnresolvable in backend
        if (configMap.containsKey("includeNS") && !configMap.containsKey("includeUnresolvable")) {
            configMap["includeUnresolvable"] = configMap["includeNS"]!!
        }

        // Create AmassParams object with values from configMap, falling back to defaults when not present
        return AmassParams(
            verbose = (configMap["verbose"] as? Boolean) ?: true,
            passive = (configMap["passive"] as? Boolean) ?: true,
            timeout = (configMap["timeout"] as? Number)?.toInt() ?: 10,
            minForRecursive = (configMap["minForRecursive"] as? Number)?.toInt() ?: 1,
            includeUnresolvable = (configMap["includeUnresolvable"] as? Boolean) ?: false,
            additionalOptions = configMap["additionalOptions"] as? String
        )
    }

    private fun parseFindings(logs: List<String>, directLogs: List<String>, domain: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        val fqdnRegex = """\b(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]{2,6}\b""".toRegex()

        val allLogs = logs + directLogs

        for (logLine in allLogs) {
            logger.debug("Parsing log line: $logLine")

            // Filter out unwanted lines
            if (logLine.contains("DNS wildcard detected", ignoreCase = true) ||
                logLine.contains("netblock", ignoreCase = true) ||
                logLine.contains("rir organization", ignoreCase = true) ||
                logLine.isBlank()
            ) {
                continue
            }

            when {
                // Handle MX records
                logLine.contains("--> mx_record -->", ignoreCase = true) -> {
                    val parts = logLine.split("-->").map { it.trim() }
                    if (parts.size == 3) {
                        val mxRecordValue = extractFqdnOrIp(parts[2])
                        if (mxRecordValue.isNotBlank()) {
                            findings.add(Finding(value = mxRecordValue, type = "mx_record", ipAddress = null))
                            logger.info("Added MX Record: $mxRecordValue")
                        }
                    }
                }
                // Handle Nameserver records
                logLine.contains("--> ns_record -->", ignoreCase = true) -> {
                    val parts = logLine.split("-->").map { it.trim() }
                    if (parts.size >= 3) {
                        val nsValue = extractFqdnOrIp(parts[2])
                        if (nsValue.isNotBlank()) {
                            findings.add(Finding(value = nsValue, type = "nameserver", ipAddress = null))
                            logger.info("Added Nameserver: $nsValue")
                        }
                    }
                }
                // Handle A records which represent subdomains with IP addresses
                logLine.contains("--> a_record -->", ignoreCase = true) || logLine.contains("--> aaaa_record -->", ignoreCase = true) -> {
                    val parts = logLine.split("-->").map { it.trim() }
                    if (parts.size >= 3) {
                        val subdomainValue = extractFqdnOrIp(parts[0])
                        val ipAddressValue = extractFqdnOrIp(parts[2])
                        if (subdomainValue.isNotBlank()) {
                            // Only add the subdomain as a finding with its IP address
                            findings.add(Finding(value = subdomainValue, type = "subdomain", ipAddress = ipAddressValue.takeIf { it.isNotBlank() }))
                            logger.info("Added Subdomain: $subdomainValue with IP: ${ipAddressValue.takeIf { it.isNotBlank() } ?: "N/A"}")
                        }
                    }
                }
                // Handle CNAME records
                logLine.contains("--> cname -->", ignoreCase = true) -> {
                    val parts = logLine.split("-->").map { it.trim() }
                    if (parts.size >= 3) {
                        val cnameSource = extractFqdnOrIp(parts[0])
                        val cnameTarget = extractFqdnOrIp(parts[2])
                        if (cnameSource.isNotBlank() && cnameTarget.isNotBlank()) {
                            findings.add(Finding(value = cnameSource, type = "cname", ipAddress = null, cnameValue = cnameTarget))
                            logger.info("Added CNAME: $cnameSource points to $cnameTarget")
                        }
                    }
                }
                // Handle lines that are just FQDNs, likely subdomains
                fqdnRegex.matches(logLine.trim()) -> {
                    val subdomain = logLine.trim()
                    findings.add(Finding(value = subdomain, type = "subdomain", ipAddress = null))
                    logger.info("Added Subdomain from direct match: $subdomain")
                }
                else -> {
                    logger.debug("Log line did not match any known pattern: $logLine")
                }
            }
        }

        // Deduplicate findings based on value and type
        return findings.distinctBy { Pair(it.value.toLowerCase(), it.type.toLowerCase()) }
    }

    // Helper function to extract FQDN or IP from a string that might contain extra info like (FQDN) or (IPAddress)
    private fun extractFqdnOrIp(input: String): String {
        val fqdnMatch = """\b(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]{2,6}\b""".toRegex().find(input)
        if (fqdnMatch != null) {
            return fqdnMatch.value
        }
        val ipMatch = """\b(?:\d{1,3}\.){3}\d{1,3}\b""".toRegex().find(input)
        if (ipMatch != null) {
            return ipMatch.value
        }
        return "" // Return empty if neither is found
    }

    private fun generateFindingSummaries(findings: List<Finding>): List<String> {
        val subdomains = findings.filter { it.type == "subdomain" }
        val nameservers = findings.filter { it.type == "nameserver" }
        val mxRecords = findings.filter { it.type == "mx_record" }
        val cnameRecords = findings.filter { it.type == "cname" }
        val errors = findings.filter { it.type == "error" }

        return buildList {
            add("Found ${findings.count { it.type != "error" }} total actionable items:")
            add("- ${subdomains.size} subdomains")
            add("- ${nameservers.size} nameservers")
            add("- ${mxRecords.size} MX records")
            add("- ${cnameRecords.size} CNAME records")
            if (errors.isNotEmpty()) {
                add("Scan also reported ${errors.size} errors/warnings:")
                errors.take(3).forEach { error -> add("  - ${error.value.take(150)}") }
                if (errors.size > 3) add("  - ... and ${errors.size - 3} more errors/warnings (check logs).")
            }
        }
    }

    @Transactional
    fun saveFindingsToDatabase(scanEntity: ScanEntity, findings: List<FindingEntity>) {
        if (findings.isNotEmpty()) {
            findingRepository.saveAll(findings)
            logger.info("Successfully saved ${findings.size} findings for scan ID ${scanEntity.id}")
        } else {
            logger.info("No findings to save for scan ID ${scanEntity.id}")
        }
    }

    private fun getContainerLogs(containerId: String): List<String> {
        val logs = mutableListOf<String>()
        try {
            dockerClient.logContainerCmd(containerId)
                .withStdOut(true)
                .withStdErr(true)
                .withTailAll()
                .exec(object : ResultCallback.Adapter<Frame>() {
                    override fun onNext(frame: Frame) {
                        logs.add(String(frame.payload).trim())
                    }
                }).awaitCompletion(30, TimeUnit.SECONDS)
        } catch (e: Exception) {
            logger.error("Error getting logs from container $containerId: ${e.message}", e)
        }
        return logs.filter { it.isNotBlank() }
    }

    private fun streamContainerLogs(containerId: String, lineCallback: (String) -> Unit): Future<*> {
        return executor.submit {
            try {
                logger.debug("Starting log stream for container $containerId")
                dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .withTailAll()
                    .exec(object : ResultCallback.Adapter<Frame>() {
                        override fun onNext(frame: Frame) {
                            String(frame.payload).trim().split('\n').forEach { line ->
                                if (line.isNotBlank()) {
                                    lineCallback(line)
                                }
                            }
                        }
                        override fun onError(throwable: Throwable) {
                            logger.warn("Error during log streaming for container $containerId: ${throwable.message}", throwable)
                            super.onError(throwable)
                        }
                        override fun onComplete() {
                            logger.debug("Log streaming completed for container $containerId")
                            super.onComplete()
                        }
                    }).awaitCompletion()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                logger.info("Log streaming for container $containerId was interrupted.")
            } catch (e: Exception) {
                if (!(e is NotModifiedException || e is NotFoundException)) {
                    logger.error("Error setting up or during log streaming for container $containerId: ${e.message}", e)
                } else {
                    logger.debug("Log streaming for container $containerId faced non-critical error (NotFound/NotModified): ${e.message}")
                }
            } finally {
                logger.debug("Exiting log streaming thread for container $containerId")
            }
        }
    }
}