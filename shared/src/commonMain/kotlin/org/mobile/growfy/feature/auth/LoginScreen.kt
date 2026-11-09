package org.mobile.growfy.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.ui.platform.testTag
import org.mobile.growfy.core.ui.TestTags
import org.mobile.growfy.core.ui.icons.AppIcons
import org.mobile.growfy.core.ui.theme.GrowfyColors
import org.mobile.growfy.navigation.Route

// Not in the design: the backend signs in with a phone code before Google can be connected.
// Laid out like a1 (Connect Google) so the two read as one flow.
@Composable
fun LoginScreen(onSignedIn: (Route) -> Unit, viewModel: LoginViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.destination) { state.destination?.let(onSignedIn) }
    val phoneFocus = remember { FocusRequester() }
    val codeFocus = remember { FocusRequester() }
    LaunchedEffect(state.codeSentTo) { (if (state.codeSentTo == null) phoneFocus else codeFocus).requestFocus() }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding()
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 28.dp),
    ) {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(GrowfyColors.Ink),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Logo, null, Modifier.size(24.dp), tint = Color.White)
        }
        Spacer(Modifier.height(36.dp))
        val sentTo = state.codeSentTo
        Text(if (sentTo == null) "Sign in" else "Enter the code", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            if (sentTo == null) "We’ll text you a 6-digit code." else "Sent to $sentTo",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        if (sentTo == null) {
            OutlinedTextField(
                value = state.phone,
                onValueChange = viewModel::setPhone,
                label = { Text("Phone number") },
                singleLine = true,
                isError = state.error != null,
                supportingText = state.error?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (state.canSendCode) viewModel.sendCode() }),
                modifier = Modifier.fillMaxWidth().focusRequester(phoneFocus).testTag(TestTags.LOGIN_PHONE),
            )
        } else {
            OutlinedTextField(
                value = state.code,
                onValueChange = viewModel::setCode,
                label = { Text("Code") },
                singleLine = true,
                isError = state.error != null,
                supportingText = state.error?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.verify() }),
                modifier = Modifier.fillMaxWidth().focusRequester(codeFocus).testTag(TestTags.LOGIN_CODE),
            )
            Row {
                TextButton(onClick = viewModel::sendCode, enabled = state.resendIn == 0 && !state.loading) {
                    Text(if (state.resendIn > 0) "Resend in ${state.resendIn}s" else "Resend code")
                }
                TextButton(onClick = viewModel::changeNumber) { Text("Change number") }
            }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = { if (sentTo == null) viewModel.sendCode() else viewModel.verify() },
            enabled = if (sentTo == null) state.canSendCode else state.canVerify,
            modifier = Modifier.fillMaxWidth().height(56.dp).testTag(TestTags.LOGIN_SUBMIT),
            shape = RoundedCornerShape(28.dp),
        ) {
            if (state.loading) {
                CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(if (sentTo == null) "Send code" else "Continue", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun Row(content: @Composable () -> Unit) =
    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { content() }
