package com.zapret.app.presentation.quickstart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zapret.app.domain.model.OperationResult
import com.zapret.app.domain.repository.ArtifactIntegrityRepository
import com.zapret.app.domain.repository.ProfileRepository
import com.zapret.app.domain.repository.QuickStartRepository
import com.zapret.app.domain.usecase.DiagnosticsUseCase
import com.zapret.app.domain.usecase.StartProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QuickStartViewModel(
    private val profileRepository: ProfileRepository,
    private val quickStartRepository: QuickStartRepository,
    private val artifactIntegrityRepository: ArtifactIntegrityRepository,
    private val startProfileUseCase: StartProfileUseCase,
    private val diagnosticsUseCase: DiagnosticsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(QuickStartState())
    val state: StateFlow<QuickStartState> = _state.asStateFlow()

    fun dispatch(intent: QuickStartIntent) {
        when (intent) {
            QuickStartIntent.Load -> load()
            is QuickStartIntent.SelectProfile -> selectProfile(intent.profileId)
            QuickStartIntent.StartOneTap -> startOneTap()
            QuickStartIntent.RunDiagnostics -> runDiagnostics()
            is QuickStartIntent.Remediate -> remediate(intent.deepLink)
            QuickStartIntent.RemediationHandled -> remediationHandled()
        }
    }

    private fun load() {
        viewModelScope.launch {
            val profiles = profileRepository.getAvailableProfiles()
            val selected = profileRepository.getSelectedProfile()
            val quickStart = quickStartRepository.checkPrerequisites()
            val indicators = artifactIntegrityRepository.getIndicators()
            _state.value = _state.value.copy(
                loading = false,
                prerequisites = quickStart,
                profiles = profiles,
                selectedProfileId = selected?.id,
                integrityIndicators = indicators,
                error = null,
            )
        }
    }

    private fun selectProfile(profileId: String) {
        viewModelScope.launch {
            val result = profileRepository.selectProfile(profileId)
            _state.value = if (result is OperationResult.Failure) {
                _state.value.copy(error = result.reason)
            } else {
                _state.value.copy(selectedProfileId = profileId, error = null)
            }
        }
    }

    private fun startOneTap() {
        viewModelScope.launch {
            val result = startProfileUseCase(_state.value.selectedProfileId)
            _state.value = _state.value.copy(startResult = result, error = (result as? OperationResult.Failure)?.reason)
        }
    }

    private fun runDiagnostics() {
        viewModelScope.launch {
            val report = diagnosticsUseCase()
            _state.value = _state.value.copy(
                diagnostics = report,
                diagnosticsSummary = report.summary,
            )
        }
    }

    private fun remediate(deepLink: String) {
        _state.value = _state.value.copy(pendingRemediationDeepLink = deepLink)
    }

    private fun remediationHandled() {
        _state.value = _state.value.copy(pendingRemediationDeepLink = null)
    }
}
