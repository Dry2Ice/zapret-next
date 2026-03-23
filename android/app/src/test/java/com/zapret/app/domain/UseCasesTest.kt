package com.zapret.app.domain

import com.zapret.app.data.repository.InMemoryProfileRepository
import com.zapret.app.data.repository.QuickStartRepositoryImpl
import com.zapret.app.domain.model.StrategyProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UseCasesTest {

    @Test
    fun `quick start fails when no profile selected`() = runBlocking {
        val profileRepo = InMemoryProfileRepository()
        val quickStartRepository = QuickStartRepositoryImpl(
            vpnPermissionProvider = { true },
            batteryOptimizationProvider = { true },
            profileRepository = profileRepo,
        )

        val result = quickStartRepository.checkPrerequisites()
        assertFalse(result.canStartNow)

        profileRepo.install(StrategyProfile("general", "General", 100, "1.0.0"))
        val next = quickStartRepository.checkPrerequisites()
        assertTrue(next.canStartNow)
    }
}
