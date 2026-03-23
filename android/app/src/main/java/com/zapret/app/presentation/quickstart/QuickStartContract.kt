package com.zapret.app.presentation.quickstart

import com.zapret.app.domain.model.DataOriginIntegrityIndicator
import com.zapret.app.domain.model.DiagnosticReport
import com.zapret.app.domain.model.OperationResult
import com.zapret.app.domain.model.QuickStartCheckResult
import com.zapret.app.domain.model.StrategyProfile

data class QuickStartState(
    val loading: Boolean = true,
    val prerequisites: QuickStartCheckResult? = null,
    val profiles: List<StrategyProfile> = emptyList(),
    val selectedProfileId: String? = null,
    val integrityIndicators: List<DataOriginIntegrityIndicator> = emptyList(),
    val diagnostics: DiagnosticReport? = null,
    val diagnosticsSummary: String? = null,
    val pendingRemediationDeepLink: String? = null,
    val error: String? = null,
    val startResult: OperationResult? = null,
)

sealed interface QuickStartIntent {
    data object Load : QuickStartIntent
    data class SelectProfile(val profileId: String) : QuickStartIntent
    data object StartOneTap : QuickStartIntent
    data object RunDiagnostics : QuickStartIntent
    data class Remediate(val deepLink: String) : QuickStartIntent
    data object RemediationHandled : QuickStartIntent
}
