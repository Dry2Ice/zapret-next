package com.zapret.app.domain.repository

import com.zapret.app.domain.model.DiagnosticReport

import com.zapret.app.domain.model.DataOriginIntegrityIndicator
import com.zapret.app.domain.model.OperationResult
import com.zapret.app.domain.model.QuickStartCheckResult
import com.zapret.app.domain.model.ServiceStatus
import com.zapret.app.domain.model.StrategyProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    suspend fun install(profile: StrategyProfile): OperationResult
    suspend fun getAvailableProfiles(): List<StrategyProfile>
    suspend fun getSelectedProfile(): StrategyProfile?
    suspend fun selectProfile(profileId: String): OperationResult
}

interface TunnelControlRepository {
    suspend fun start(profileId: String): OperationResult
    suspend fun stop(): OperationResult
}

interface DiagnosticsRepository {
    suspend fun runDiagnostics(): DiagnosticReport
}

interface StatusRepository {
    fun observeStatus(): Flow<ServiceStatus>
    suspend fun getCurrentStatus(): ServiceStatus
}

interface ListsRepository {
    suspend fun updateLists(): OperationResult
    suspend fun exportLegacyLists(): OperationResult
    suspend fun importLegacyLists(): OperationResult
}

interface QuickStartRepository {
    suspend fun checkPrerequisites(): QuickStartCheckResult
}


interface ArtifactIntegrityRepository {
    suspend fun getIndicators(): List<DataOriginIntegrityIndicator>
}
