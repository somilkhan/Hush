package app.hush.placeholder.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.hush.placeholder.core.domain.account.AuthRepository
import app.hush.placeholder.design.HushSpacing
import app.hush.placeholder.design.HushTokens
import kotlinx.coroutines.launch

/** Account, sources and licence information. */
@Composable
fun SettingsScreen(
    auth: AuthRepository,
    onSignIn: () -> Unit,
    onSignedOut: () -> Unit,
    onManageSources: () -> Unit,
) {
    val colors = HushTokens.colors
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        email = auth.currentSession()?.email
        loaded = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = HushSpacing.gutter, vertical = HushSpacing.s16),
        verticalArrangement = Arrangement.spacedBy(HushSpacing.s12),
    ) {
        SectionTitle("Account")
        Text(
            text = when {
                !loaded -> "Checking account"
                email != null -> "Signed in as $email"
                else -> "Guest mode"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = colors.label,
        )
        Text(
            text = if (email == null) {
                "Your library and history stay on this device. Sign in to link an account."
            } else {
                "Account sync is not active yet. Your data is still stored on this device."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.labelSecondary,
        )
        if (email != null) {
            OutlinedButton(
                onClick = { scope.launch { auth.signOut(); onSignedOut() } },
                modifier = Modifier.defaultMinSize(minHeight = HushSpacing.minTouch),
            ) { Text("Sign out") }
        } else {
            OutlinedButton(
                onClick = onSignIn,
                modifier = Modifier.defaultMinSize(minHeight = HushSpacing.minTouch),
            ) { Text("Sign in") }
        }

        SectionTitle("Sources")
        Text(
            text = "Addons supply titles, search and streams. Add one by its manifest link.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.labelSecondary,
        )
        OutlinedButton(
            onClick = onManageSources,
            modifier = Modifier.defaultMinSize(minHeight = HushSpacing.minTouch),
        ) { Text("Manage sources") }

        SectionTitle("About")
        Text(
            text = "HUSH is open source under GPL-3.0. The name is a working name and is not final.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.labelSecondary,
        )
        Text(
            text = "Screen structure is inspired by NuvioMobile-Enhanced. No code, artwork or logos were copied from it.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.labelSecondary,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = HushTokens.colors.label,
        modifier = Modifier.fillMaxWidth(),
    )
}
