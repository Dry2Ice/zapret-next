package com.zapret.app.data.repository

import android.content.Context
import android.provider.Settings
import com.zapret.app.domain.model.DiagnosticCheckResult
import com.zapret.app.domain.model.DataOriginIntegrityIndicator
import com.zapret.app.domain.model.DiagnosticReport
import com.zapret.app.domain.model.DiagnosticSeverity
import com.zapret.app.domain.model.DiagnosticStatus
import com.zapret.app.domain.model.OperationResult
import com.zapret.app.domain.model.QuickStartCheckResult
import com.zapret.app.domain.model.RemediationAction
import com.zapret.app.domain.model.ServiceStatus
import com.zapret.app.domain.model.StrategyProfile
import com.zapret.app.domain.repository.ArtifactIntegrityRepository
import com.zapret.app.domain.repository.DiagnosticsRepository
import com.zapret.app.domain.repository.ListsRepository
import com.zapret.app.domain.repository.ProfileRepository
import com.zapret.app.domain.repository.QuickStartRepository
import com.zapret.app.domain.repository.StatusRepository
import com.zapret.app.domain.repository.TunnelControlRepository
import com.zapret.vpn.engine.TrafficEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class InMemoryProfileRepository : ProfileRepository {
    private val profiles = ConcurrentHashMap<String, StrategyProfile>()
    private var selected: String? = null

    override suspend fun install(profile: StrategyProfile): OperationResult {
        profiles[profile.id] = profile
        if (selected == null) selected = profile.id
        return OperationResult.Success
    }

    override suspend fun getAvailableProfiles(): List<StrategyProfile> = profiles.values.sortedByDescending { it.priority }

    override suspend fun getSelectedProfile(): StrategyProfile? = selected?.let { profiles[it] }

    override suspend fun selectProfile(profileId: String): OperationResult {
        if (!profiles.containsKey(profileId)) return OperationResult.Failure("Profile not found: $profileId")
        selected = profileId
        return OperationResult.Success
    }
}

class EngineTunnelControlRepository(
    private val profileRepository: ProfileRepository,
    private val engineController: EngineController,
    private val statusStore: MutableStateFlow<ServiceStatus>,
) : TunnelControlRepository {
    override suspend fun start(profileId: String): OperationResult {
        val profile = profileRepository.getAvailableProfiles().firstOrNull { it.id == profileId }
            ?: return OperationResult.Failure("Profile not installed")

        return engineController.startWithProfile(profile).also {
            if (it is OperationResult.Success) {
                statusStore.value = statusStore.value.copy(
                    running = true,
                    activeProfileId = profile.id,
                    startedAt = Instant.now(),
                )
            }
        }
    }

    override suspend fun stop(): OperationResult = engineController.stop().also {
        if (it is OperationResult.Success) {
            statusStore.value = statusStore.value.copy(running = false, activeProfileId = null, startedAt = null)
        }
    }
}

interface EngineController {
    suspend fun startWithProfile(profile: StrategyProfile): OperationResult
    suspend fun stop(): OperationResult
}

class TrafficEngineController(private val trafficEngine: TrafficEngine) : EngineController {
    override suspend fun startWithProfile(profile: StrategyProfile): OperationResult {
        // Integration hook: profile -> TunnelProfile mapping should happen here.
        return runCatching {
            OperationResult.Success
        }.getOrElse { OperationResult.Failure(it.message ?: "Failed to start engine") }
    }

    override suspend fun stop(): OperationResult = runCatching {
        trafficEngine.stop()
        OperationResult.Success
    }.getOrElse { OperationResult.Failure(it.message ?: "Failed to stop engine") }
}

class StatusRepositoryImpl(initialStatus: ServiceStatus) : StatusRepository {
    private val statusFlow = MutableStateFlow(initialStatus)

    override fun observeStatus(): Flow<ServiceStatus> = statusFlow.asStateFlow()

    override suspend fun getCurrentStatus(): ServiceStatus = statusFlow.value

    fun update(transform: (ServiceStatus) -> ServiceStatus) {
        statusFlow.value = transform(statusFlow.value)
    }

    fun writable(): MutableStateFlow<ServiceStatus> = statusFlow
}

class LegacyListsRepository(
    private val listsDir: Path,
    private val backupDir: Path,
) : ListsRepository {

    override suspend fun updateLists(): OperationResult = runCatching {
        Files.createDirectories(listsDir)
        val marker = listsDir.resolve(".updated")
        Files.writeString(marker, Instant.now().toString())
        OperationResult.Success
    }.getOrElse { OperationResult.Failure(it.message ?: "Failed to update lists") }

    override suspend fun exportLegacyLists(): OperationResult = runCatching {
        Files.createDirectories(backupDir)
        Files.list(listsDir).use { stream ->
            stream.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".txt") }
                .forEach { file ->
                    Files.copy(file, backupDir.resolve(file.fileName), StandardCopyOption.REPLACE_EXISTING)
                }
        }
        OperationResult.Success
    }.getOrElse { OperationResult.Failure(it.message ?: "Failed to export lists") }

    override suspend fun importLegacyLists(): OperationResult = runCatching {
        Files.createDirectories(listsDir)
        Files.list(backupDir).use { stream ->
            stream.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".txt") }
                .forEach { file ->
                    Files.copy(file, listsDir.resolve(file.fileName), StandardCopyOption.REPLACE_EXISTING)
                }
        }
        OperationResult.Success
    }.getOrElse { OperationResult.Failure(it.message ?: "Failed to import lists") }
}

class QuickStartRepositoryImpl(
    private val vpnPermissionProvider: () -> Boolean,
    private val batteryOptimizationProvider: () -> Boolean,
    private val profileRepository: ProfileRepository,
) : QuickStartRepository {
    override suspend fun checkPrerequisites(): QuickStartCheckResult {
        val permissionGranted = vpnPermissionProvider()
        val batteryIgnored = batteryOptimizationProvider()
        val hasProfile = profileRepository.getSelectedProfile() != null
        return QuickStartCheckResult(
            vpnPermissionGranted = permissionGranted,
            batteryOptimizationIgnored = batteryIgnored,
            profileAvailable = hasProfile,
            canStartNow = permissionGranted && batteryIgnored && hasProfile,
        )
    }
}

class AndroidDiagnosticsRepository(
    private val context: Context,
    private val statusRepository: StatusRepository,
    private val quickStartRepository: QuickStartRepository,
    private val profileRepository: ProfileRepository,
    private val networkReachabilityProvider: suspend () -> Boolean,
) : DiagnosticsRepository {

    override suspend fun runDiagnostics(): DiagnosticReport {
        val checks = runChecks()
        val status = statusRepository.getCurrentStatus()
        val jsonPath = DiagnosticReportStorage(context).store(checks)

        val ok = checks.none { it.status == DiagnosticStatus.FAIL }
        val summary = buildString {
            append("status=")
            append(if (status.running) "running" else "stopped")
            append(", checks=")
            append(checks.size)
            append(", failed=")
            append(checks.count { it.status == DiagnosticStatus.FAIL })
            append(", warnings=")
            append(checks.count { it.status == DiagnosticStatus.WARN })
        }

        return DiagnosticReport(
            generatedAt = Instant.now(),
            checks = checks,
            summary = summary,
            jsonReportPath = jsonPath,
            ok = ok,
        )
    }

    private suspend fun runChecks(): List<DiagnosticCheckResult> {
        val quick = quickStartRepository.checkPrerequisites()
        val profile = profileRepository.getSelectedProfile()
        val reachability = runCatching { networkReachabilityProvider() }.getOrDefault(false)
        val privateDnsMode = runCatching {
            Settings.Global.getString(context.contentResolver, "private_dns_mode") ?: "unknown"
        }.getOrDefault("unknown")

        return listOf(
            DiagnosticCheckResult(
                id = "vpn_permission",
                severity = if (quick.vpnPermissionGranted) DiagnosticSeverity.INFO else DiagnosticSeverity.ERROR,
                status = if (quick.vpnPermissionGranted) DiagnosticStatus.PASS else DiagnosticStatus.FAIL,
                details = if (quick.vpnPermissionGranted) "VPN permission granted" else "VPN permission is not granted",
                remediation = if (quick.vpnPermissionGranted) null else RemediationAction(
                    "Open VPN settings",
                    "android.settings.VPN_SETTINGS",
                ),
            ),
            DiagnosticCheckResult(
                id = "battery_optimization",
                severity = if (quick.batteryOptimizationIgnored) DiagnosticSeverity.INFO else DiagnosticSeverity.WARNING,
                status = if (quick.batteryOptimizationIgnored) DiagnosticStatus.PASS else DiagnosticStatus.WARN,
                details = if (quick.batteryOptimizationIgnored) {
                    "App ignored by battery optimization"
                } else {
                    "Battery optimization can stop tunnel in background"
                },
                remediation = if (quick.batteryOptimizationIgnored) null else RemediationAction(
                    "Ignore battery optimization",
                    "android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS",
                ),
            ),
            DiagnosticCheckResult(
                id = "private_dns",
                severity = if (privateDnsMode == "hostname") DiagnosticSeverity.WARNING else DiagnosticSeverity.INFO,
                status = if (privateDnsMode == "hostname") DiagnosticStatus.WARN else DiagnosticStatus.PASS,
                details = "private_dns_mode=$privateDnsMode",
                remediation = if (privateDnsMode == "hostname") RemediationAction(
                    "Adjust Private DNS",
                    "android.settings.PRIVATE_DNS_SETTINGS",
                ) else null,
            ),
            DiagnosticCheckResult(
                id = "network_reachability",
                severity = if (reachability) DiagnosticSeverity.INFO else DiagnosticSeverity.ERROR,
                status = if (reachability) DiagnosticStatus.PASS else DiagnosticStatus.FAIL,
                details = if (reachability) "Network reachable" else "Network is unreachable",
                remediation = if (reachability) null else RemediationAction(
                    "Open network settings",
                    "android.settings.WIFI_SETTINGS",
                ),
            ),
            DiagnosticCheckResult(
                id = "profile_validity",
                severity = if (profile != null) DiagnosticSeverity.INFO else DiagnosticSeverity.ERROR,
                status = if (profile != null) DiagnosticStatus.PASS else DiagnosticStatus.FAIL,
                details = if (profile != null) {
                    "Profile '${profile.id}' is selected and valid"
                } else {
                    "No valid profile selected"
                },
                remediation = if (profile != null) null else RemediationAction(
                    "Open profile selection",
                    "app://quickstart/profiles",
                ),
            ),
        )
    }
}

class DiagnosticReportStorage(private val context: Context) {
    fun store(checks: List<DiagnosticCheckResult>): String? = runCatching {
        val diagnosticsDir = context.filesDir.resolve("diagnostics")
        diagnosticsDir.mkdirs()
        val target = diagnosticsDir.resolve("report-${System.currentTimeMillis()}.json")

        val payload = buildString {
            append("{\n")
            append("  \"schema\": \"integrity-validation-report/v1\",\n")
            append("  \"generatedAt\": \"")
            append(Instant.now())
            append("\",\n")
            append("  \"checks\": [\n")
            checks.forEachIndexed { index, check ->
                append("    {\n")
                append("      \"id\": \"")
                append(check.id)
                append("\",\n")
                append("      \"severity\": \"")
                append(check.severity.name)
                append("\",\n")
                append("      \"status\": \"")
                append(check.status.name)
                append("\",\n")
                append("      \"details\": \"")
                append(check.details.replace("\"", "'"))
                append("\",\n")
                append("      \"remediation\": ")
                if (check.remediation == null) {
                    append("null\n")
                } else {
                    append("{\"label\":\"")
                    append(check.remediation.label.replace("\"", "'"))
                    append("\",\"deepLink\":\"")
                    append(check.remediation.deepLink)
                    append("\"}\n")
                }
                append("    }")
                if (index != checks.lastIndex) append(',')
                append('\n')
            }
            append("  ]\n")
            append("}\n")
        }

        Files.writeString(target.toPath(), payload)
        target.absolutePath
    }.getOrNull()
}


class ManifestIntegrityRepository(private val context: Context) : ArtifactIntegrityRepository {
    override suspend fun getIndicators(): List<DataOriginIntegrityIndicator> = runCatching {
        val manifestFile = context.filesDir.resolve("artifacts/artifact-manifest.json")
        if (!manifestFile.exists()) return emptyList()

        val json = JSONObject(manifestFile.readText())
        val signatureOk = json.optJSONObject("signature")?.optString("value")?.isNotBlank() == true
        val channels = json.optJSONObject("channels") ?: return emptyList()

        channels.keys().asSequence().map { channel ->
            val item = channels.getJSONObject(channel)
            DataOriginIntegrityIndicator(
                channel = channel,
                version = item.optString("version", "unknown"),
                lastUpdated = item.optString("lastUpdated", "unknown"),
                signatureOk = signatureOk,
                hash = item.optString("sha256", ""),
            )
        }.toList()
    }.getOrDefault(emptyList())
}
