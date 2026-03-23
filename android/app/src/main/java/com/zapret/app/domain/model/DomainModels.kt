package com.zapret.app.domain.model

import java.time.Instant

data class StrategyProfile(
    val id: String,
    val title: String,
    val priority: Int,
    val specVersion: String,
)

data class ServiceStatus(
    val running: Boolean,
    val activeProfileId: String?,
    val startedAt: Instant?,
    val uptimeSec: Long,
    val bytesIn: Long,
    val bytesOut: Long,
)

enum class DiagnosticSeverity { INFO, WARNING, ERROR }

enum class DiagnosticStatus { PASS, WARN, FAIL, SKIPPED }

data class RemediationAction(
    val label: String,
    val deepLink: String,
)

/**
 * Canonical diagnostics model for UI + persisted JSON reports.
 */
data class DiagnosticCheckResult(
    val id: String,
    val severity: DiagnosticSeverity,
    val status: DiagnosticStatus,
    val details: String,
    val remediation: RemediationAction? = null,
)

data class DiagnosticReport(
    val generatedAt: Instant,
    val checks: List<DiagnosticCheckResult>,
    val summary: String,
    val jsonReportPath: String?,
    val ok: Boolean,
)

data class DataOriginIntegrityIndicator(
    val channel: String,
    val version: String,
    val lastUpdated: String,
    val signatureOk: Boolean,
    val hash: String,
)

data class QuickStartCheckResult(
    val vpnPermissionGranted: Boolean,
    val batteryOptimizationIgnored: Boolean,
    val profileAvailable: Boolean,
    val canStartNow: Boolean,
)

sealed interface OperationResult {
    data object Success : OperationResult
    data class Failure(val reason: String) : OperationResult
}
