package app.hush.placeholder.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import app.hush.placeholder.core.domain.account.AuthRepository
import app.hush.placeholder.design.HushSpacing
import app.hush.placeholder.design.HushTokens
import kotlinx.coroutines.launch

/**
 * Sign in, create account, or continue as guest. Password is never saved in state across process death.
 */
@Composable
fun LoginScreen(
    auth: AuthRepository,
    onSignedIn: () -> Unit,
    onGuest: () -> Unit,
) {
    val colors = HushTokens.colors
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    val canSubmit = !busy && email.isNotBlank() && password.length >= 6

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = HushSpacing.gutter, vertical = HushSpacing.s16),
        verticalArrangement = Arrangement.spacedBy(HushSpacing.s12),
    ) {
        Text(text = "HUSH", style = MaterialTheme.typography.headlineLarge, color = colors.label)
        Text(
            text = "Sign in to sync your library and history. Or continue as a guest, with everything kept on this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.labelSecondary,
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = HushSpacing.minTouch),
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = HushSpacing.minTouch),
            label = { Text("Password (6+ characters)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        error?.let { Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        info?.let { Text(text = it, style = MaterialTheme.typography.bodySmall, color = colors.labelSecondary) }

        Button(
            onClick = {
                scope.launch {
                    busy = true
                    error = null
                    auth.signIn(email.trim(), password)
                        .onSuccess { onSignedIn() }
                        .onFailure { error = it.message ?: "Sign-in failed" }
                    busy = false
                }
            },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = HushSpacing.minTouch),
        ) { Text("Sign in") }

        OutlinedButton(
            onClick = {
                scope.launch {
                    busy = true
                    error = null
                    info = null
                    auth.signUp(email.trim(), password)
                        .onSuccess { session ->
                            if (session != null) onSignedIn() else info = "Check your email to confirm your account, then sign in."
                        }
                        .onFailure { error = it.message ?: "Could not create account" }
                    busy = false
                }
            },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = HushSpacing.minTouch),
        ) { Text("Create account") }

        TextButton(
            onClick = onGuest,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = HushSpacing.minTouch),
        ) { Text("Continue as guest") }
    }
}
