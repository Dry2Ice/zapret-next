package com.zapret.app.domain.usecase

import com.zapret.app.domain.model.DiagnosticReport
import com.zapret.app.domain.model.OperationResult
import com.zapret.app.domain.model.ServiceStatus
import com.zapret.app.domain.model.StrategyProfile
import com.zapret.app.domain.repository.DiagnosticsRepository
import com.zapret.app.domain.repository.ListsRepository
import com.zapret.app.domain.repository.ProfileRepository
import com.zapret.app.domain.repository.StatusRepository
import com.zapret.app.domain.repository.TunnelControlRepository
import kotlinx.coroutines.flow.Flow

class InstallProfileUseCase(private val profileRepository: ProfileRepository) {
    suspend operator fun invoke(profile: StrategyProfile): OperationResult = profileRepository.install(profile)
}

class StartProfileUseCase(
    private val profileRepository: ProfileRepository,
    private val tunnelControlRepository: TunnelControlRepository,
) {
    suspend operator fun invoke(profileId: String? = null): OperationResult {
        val target = profileId ?: profileRepository.getSelectedProfile()?.id
            ?: return OperationResult.Failure("No selected profile")
        return tunnelControlRepository.start(target)
    }
}

class StopProfileUseCase(private val tunnelControlRepository: TunnelControlRepository) {
    suspend operator fun invoke(): OperationResult = tunnelControlRepository.stop()
}

class DiagnosticsUseCase(private val diagnosticsRepository: DiagnosticsRepository) {
    suspend operator fun invoke(): DiagnosticReport = diagnosticsRepository.runDiagnostics()
}

class UpdateListsUseCase(private val listsRepository: ListsRepository) {
    suspend operator fun invoke(): OperationResult = listsRepository.updateLists()
}

class ExportLegacyListsUseCase(private val listsRepository: ListsRepository) {
    suspend operator fun invoke(): OperationResult = listsRepository.exportLegacyLists()
}

class ImportLegacyListsUseCase(private val listsRepository: ListsRepository) {
    suspend operator fun invoke(): OperationResult = listsRepository.importLegacyLists()
}

class CheckStatusUseCase(private val statusRepository: StatusRepository) {
    operator fun invoke(): Flow<ServiceStatus> = statusRepository.observeStatus()
}
