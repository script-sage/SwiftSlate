package com.musheer360.swiftslate.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musheer360.swiftslate.BuildConfig
import com.musheer360.swiftslate.R
import com.musheer360.swiftslate.api.GeminiClient
import com.musheer360.swiftslate.api.OpenAICompatibleClient
import com.musheer360.swiftslate.manager.CommandManager
import com.musheer360.swiftslate.manager.KeyManager
import com.musheer360.swiftslate.ui.components.ScreenTitle
import com.musheer360.swiftslate.ui.components.SlateCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()

    // KeyManager
    val keyManager = remember { KeyManager(context) }
    var keys by remember { mutableStateOf(keyManager.getKeys()) }
    var newKey by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testSuccess by remember { mutableStateOf(false) }
    val geminiClient = remember { GeminiClient() }
    val openAIClient = remember { OpenAICompatibleClient() }

    val validAddedMsg = stringResource(R.string.keys_valid_added)
    val alreadyAddedMsg = stringResource(R.string.keys_already_added)
    val validationFailedMsg = stringResource(R.string.keys_validation_failed)

    // Provider
    var providerType by remember { mutableStateOf(prefs.getString("provider_type", "gemini") ?: "gemini") }
    var providerExpanded by remember { mutableStateOf(false) }

    // Gemini settings
    var selectedModel by remember { mutableStateOf(prefs.getString("model", "gemini-2.5-flash-lite") ?: "gemini-2.5-flash-lite") }
    var modelExpanded by remember { mutableStateOf(false) }
    val geminiModels = listOf("gemini-2.5-flash-lite", "gemini-3-flash-preview", "gemini-3.1-flash-lite-preview")

    // Custom provider settings
    var customEndpoint by remember { mutableStateOf(prefs.getString("custom_endpoint", "") ?: "") }
    var customModel by remember { mutableStateOf(prefs.getString("custom_model", "") ?: "") }
    var endpointError by remember { mutableStateOf<String?>(null) }

    // Trigger prefix
    val commandManager = remember { CommandManager(context) }
    var triggerPrefix by remember { mutableStateOf(commandManager.getTriggerPrefix()) }
    var prefixError by remember { mutableStateOf<String?>(null) }
    var fileSharePrefix by remember { mutableStateOf(commandManager.getFileSharePrefix()) }
    var fileSharePrefixError by remember { mutableStateOf<String?>(null) }
    var textReplacerPrefix by remember { mutableStateOf(commandManager.getTextReplacerPrefix()) }
    var textReplacerPrefixError by remember { mutableStateOf<String?>(null) }

    val prefixErrorLength = stringResource(R.string.settings_prefix_error_length)
    val prefixErrorWhitespace = stringResource(R.string.settings_prefix_error_whitespace)
    val prefixErrorAlphanumeric = stringResource(R.string.settings_prefix_error_alphanumeric)
    val endpointErrorScheme = stringResource(R.string.settings_endpoint_error_scheme)
    val endpointErrorSpaces = stringResource(R.string.settings_endpoint_error_spaces)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ScreenTitle(stringResource(R.string.settings_title))

        // API KEYS SECTION
        SlateCard {
            Text(
                text = stringResource(R.string.keys_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = newKey,
                onValueChange = { newKey = it },
                label = { Text(stringResource(R.string.keys_api_key_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        if (newKey.isNotBlank()) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isTesting = true
                            testResult = null
                            scope.launch {
                                val trimmedKey = newKey.trim()
                                if (keyManager.getKeys().contains(trimmedKey)) {
                                    isTesting = false
                                    testResult = alreadyAddedMsg
                                    testSuccess = false
                                    return@launch
                                }
                                val result = if (providerType == "custom" && customEndpoint.isNotBlank()) {
                                    openAIClient.validateKey(trimmedKey, customEndpoint)
                                } else {
                                    geminiClient.validateKey(trimmedKey)
                                }
                                isTesting = false
                                if (result.isSuccess) {
                                    keyManager.addKey(trimmedKey)
                                    keys = keyManager.getKeys()
                                    newKey = ""
                                    testResult = validAddedMsg
                                    testSuccess = true
                                } else {
                                    testResult = result.exceptionOrNull()?.message ?: validationFailedMsg
                                    testSuccess = false
                                }
                            }
                        }
                    },
                    enabled = newKey.isNotBlank() && !isTesting
                ) {
                    Text(if (isTesting) stringResource(R.string.keys_testing) else stringResource(R.string.keys_add_key))
                }
            }
            if (testResult != null) {
                Text(
                    text = testResult!!,
                    color = if (testSuccess) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (keys.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    keys.forEach { key ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "••••••••" + key.takeLast(6),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    keyManager.removeKey(key)
                                    keys = keyManager.getKeys()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.keys_delete_key),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // CONFIGURATION SECTION
        SlateCard {
            Text(
                text = stringResource(R.string.settings_provider_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = providerExpanded,
                onExpandedChange = { providerExpanded = !providerExpanded }
            ) {
                OutlinedTextField(
                    value = if (providerType == "gemini") stringResource(R.string.settings_provider_gemini) else stringResource(R.string.settings_provider_custom),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                ExposedDropdownMenu(
                    expanded = providerExpanded,
                    onDismissRequest = { providerExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_provider_gemini)) },
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            providerType = "gemini"
                            prefs.edit().putString("provider_type", "gemini").remove("structured_output_disabled").apply()
                            providerExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_provider_custom)) },
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            providerType = "custom"
                            prefs.edit().putString("provider_type", "custom").remove("structured_output_disabled").apply()
                            providerExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (providerType == "gemini") {
            SlateCard {
                Text(
                    text = stringResource(R.string.settings_model_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = modelExpanded,
                    onExpandedChange = { modelExpanded = !modelExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedModel,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = modelExpanded,
                        onDismissRequest = { modelExpanded = false }
                    ) {
                        geminiModels.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model) },
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedModel = model
                                    prefs.edit().putString("model", model).remove("structured_output_disabled").apply()
                                    modelExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        } else {
            SlateCard {
                Text(
                    text = stringResource(R.string.settings_endpoint_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_endpoint_desc),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = customEndpoint,
                    onValueChange = {
                        customEndpoint = it
                        endpointError = when {
                            it.isBlank() -> null
                            !it.startsWith("http://") && !it.startsWith("https://") -> endpointErrorScheme
                            it.contains(" ") -> endpointErrorSpaces
                            else -> null
                        }
                        if (endpointError == null) {
                            prefs.edit().putString("custom_endpoint", it).remove("structured_output_disabled").apply()
                        }
                    },
                    placeholder = { Text(stringResource(R.string.settings_endpoint_placeholder)) },
                    singleLine = true,
                    isError = endpointError != null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                endpointError?.let { msg ->
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            SlateCard {
                Text(
                    text = stringResource(R.string.settings_model_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_model_desc),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = customModel,
                    onValueChange = {
                        customModel = it
                        prefs.edit().putString("custom_model", it).remove("structured_output_disabled").apply()
                    },
                    placeholder = { Text(stringResource(R.string.settings_model_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // PREFIXES SECTION
        SlateCard {
            Text(
                text = stringResource(R.string.settings_trigger_prefix_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_trigger_prefix_desc, triggerPrefix),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = triggerPrefix,
                onValueChange = { input ->
                    val filtered = input.take(1)
                    triggerPrefix = filtered
                    prefixError = when {
                        filtered.length != 1 -> prefixErrorLength
                        filtered[0].isWhitespace() -> prefixErrorWhitespace
                        filtered[0].isLetterOrDigit() -> prefixErrorAlphanumeric
                        else -> {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            commandManager.setTriggerPrefix(filtered)
                            null
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.width(80.dp),
                isError = prefixError != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            prefixError?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SlateCard {
            Text(
                text = stringResource(R.string.settings_file_share_prefix_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_file_share_prefix_desc, fileSharePrefix),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = fileSharePrefix,
                onValueChange = { input ->
                    val filtered = input.take(1)
                    fileSharePrefix = filtered
                    fileSharePrefixError = when {
                        filtered.length != 1 -> prefixErrorLength
                        filtered[0].isWhitespace() -> prefixErrorWhitespace
                        filtered[0].isLetterOrDigit() -> prefixErrorAlphanumeric
                        else -> {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            commandManager.setFileSharePrefix(filtered)
                            null
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.width(80.dp),
                isError = fileSharePrefixError != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            fileSharePrefixError?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SlateCard {
            Text(
                text = stringResource(R.string.settings_text_replacer_prefix_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_text_replacer_prefix_desc, textReplacerPrefix),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = textReplacerPrefix,
                onValueChange = { input ->
                    val filtered = input.take(1)
                    textReplacerPrefix = filtered
                    textReplacerPrefixError = when {
                        filtered.length != 1 -> prefixErrorLength
                        filtered[0].isWhitespace() -> prefixErrorWhitespace
                        filtered[0].isLetterOrDigit() -> prefixErrorAlphanumeric
                        else -> {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            commandManager.setTextReplacerPrefix(filtered)
                            null
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.width(80.dp),
                isError = textReplacerPrefixError != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            textReplacerPrefixError?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ABOUT & INFO SECTION
        SlateCard {
            Text(
                text = stringResource(R.string.dashboard_how_to_use_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.dashboard_how_to_use_body, triggerPrefix),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                lineHeight = 22.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.dashboard_version, BuildConfig.VERSION_NAME),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.dashboard_github),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Musheer360/SwiftSlate")))
                }
            )
        }
    }
}
