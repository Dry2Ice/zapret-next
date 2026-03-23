package com.zapret.app.presentation.quickstart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun QuickStartScreen(
    state: QuickStartState,
    onIntent: (QuickStartIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Quick Start", style = MaterialTheme.typography.headlineMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Prerequisites", style = MaterialTheme.typography.titleMedium)
                Text("VPN permission: ${state.prerequisites?.vpnPermissionGranted == true}")
                Text("Battery optimization ignored: ${state.prerequisites?.batteryOptimizationIgnored == true}")
                Text("Profile selected: ${state.selectedProfileId ?: "none"}")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Profiles", style = MaterialTheme.typography.titleMedium)
                state.profiles.forEach { profile ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "${profile.title} (${profile.id})")
                        OutlinedButton(onClick = { onIntent(QuickStartIntent.SelectProfile(profile.id)) }) {
                            Text("Use")
                        }
                    }
                }
            }
        }


        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Data integrity", style = MaterialTheme.typography.titleMedium)
                state.integrityIndicators.forEach { indicator ->
                    Text("${indicator.channel}: v${indicator.version}, updated=${indicator.lastUpdated}")
                    Text("signature=${if (indicator.signatureOk) "ok" else "invalid"}, sha256=${indicator.hash.take(12)}…")
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onIntent(QuickStartIntent.StartOneTap) }) {
                Text("Start in one tap")
            }
            OutlinedButton(onClick = { onIntent(QuickStartIntent.RunDiagnostics) }) {
                Text("Diagnostics")
            }
        }

        state.diagnosticsSummary?.let {
            Text("Summary: $it", style = MaterialTheme.typography.bodyMedium)
        }

        state.diagnostics?.let { report ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Diagnostics report", style = MaterialTheme.typography.titleMedium)
                    Text("OK: ${report.ok}")
                    Text("JSON report: ${report.jsonReportPath ?: "not persisted"}")
                    report.checks.forEach { check ->
                        Text("[${check.severity}/${check.status}] ${check.id}: ${check.details}")
                        check.remediation?.let { remediation ->
                            OutlinedButton(onClick = { onIntent(QuickStartIntent.Remediate(remediation.deepLink)) }) {
                                Text(remediation.label)
                            }
                        }
                    }
                }
            }
        }

        state.pendingRemediationDeepLink?.let {
            Text("Open settings: $it", style = MaterialTheme.typography.bodySmall)
        }

        state.error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
    }
}
