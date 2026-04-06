package com.musheer360.swiftslate.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.musheer360.swiftslate.R
import com.musheer360.swiftslate.manager.CommandManager
import com.musheer360.swiftslate.model.Command
import com.musheer360.swiftslate.model.CommandType
import com.musheer360.swiftslate.ui.components.ScreenTitle
import com.musheer360.swiftslate.ui.components.SlateCard
import kotlinx.coroutines.delay

private fun checkServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
    return enabledServices.any {
        it.resolveInfo.serviceInfo.packageName == context.packageName
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val commandManager = remember { CommandManager(context) }
    var isServiceEnabled by remember { mutableStateOf(checkServiceEnabled(context)) }
    var commands by remember { mutableStateOf(commandManager.getCommands()) }

    var isSheetOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var trigger by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf(CommandType.AI) }
    var formSelectedType by remember { mutableStateOf(CommandType.AI) }
    var editingTrigger by remember { mutableStateOf<String?>(null) }

    val aiPrefix = commandManager.getTriggerPrefix()
    val fileSharePrefix = commandManager.getFileSharePrefix()
    val textReplacerPrefix = commandManager.getTextReplacerPrefix()
    val activePrefix = when (formSelectedType) {
        CommandType.AI -> aiPrefix
        CommandType.TEXT_REPLACER -> textReplacerPrefix
        CommandType.FILE_SHARE -> fileSharePrefix
    }

    val errorPrefixMsg = stringResource(R.string.commands_error_prefix, activePrefix)
    val errorEmptyMsg = stringResource(R.string.commands_error_empty_trigger)
    val errorDuplicateMsg = stringResource(R.string.commands_error_duplicate)

    val activityLifecycle = (context as? ComponentActivity)?.lifecycle
    LaunchedEffect(activityLifecycle) {
        val lifecycle = activityLifecycle ?: return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                isServiceEnabled = checkServiceEnabled(context)
                delay(3000)
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
            } catch (e: SecurityException) {
                // Ignore failure if DocumentProvider doesn't support persistable permissions
            }
            prompt = it.toString()
            errorMessage = null
        }
    }

    fun checkFileExists(uriStr: String): Boolean {
        return try {
            val uri = Uri.parse(uriStr)
            var exists = false
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) { exists = true }
            }
            exists
        } catch (e: Exception) { false }
    }

    fun resetForm() {
        trigger = ""
        prompt = ""
        errorMessage = null
        editingTrigger = null
        formSelectedType = selectedType
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    resetForm()
                    isSheetOpen = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Command") },
                text = { Text("Add Command", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp) // Removed excess empty space above dashboard
        ) {
            ScreenTitle(stringResource(R.string.dashboard_title))

            // Service Status Card
            SlateCard {
                Text(
                    text = stringResource(R.string.service_status_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isServiceEnabled) stringResource(R.string.service_status_active) else stringResource(R.string.service_status_inactive),
                        color = if (isServiceEnabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    if (!isServiceEnabled) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Text(stringResource(R.string.service_enable), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tabs Segments
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedType == CommandType.AI,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedType = CommandType.AI
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) {
                    Text(stringResource(R.string.commands_type_ai))
                }
                SegmentedButton(
                    selected = selectedType == CommandType.TEXT_REPLACER,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedType = CommandType.TEXT_REPLACER
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) {
                    Text(stringResource(R.string.commands_type_replacer))
                }
                SegmentedButton(
                    selected = selectedType == CommandType.FILE_SHARE,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedType = CommandType.FILE_SHARE
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) {
                    Text(stringResource(R.string.commands_type_file))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Grid Command List
            val filteredCommands = commands.filter { it.type == selectedType }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 88.dp) // buffer for FAB
            ) {
                items(filteredCommands) { cmd ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = MaterialTheme.shapes.medium,
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        onClick = {
                            if (!cmd.isBuiltIn) {
                                trigger = cmd.trigger
                                prompt = cmd.prompt
                                formSelectedType = cmd.type
                                editingTrigger = cmd.trigger
                                errorMessage = null
                                isSheetOpen = true
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Custom Image Icons
                                val iconRes = when (cmd.type) {
                                    CommandType.AI -> R.drawable.ic_ai_text
                                    CommandType.TEXT_REPLACER -> R.drawable.ic_text_replacer
                                    CommandType.FILE_SHARE -> R.drawable.ic_file_share
                                }
                                Image(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                                )
                                if (cmd.isBuiltIn) {
                                    Text(
                                        text = stringResource(R.string.commands_built_in),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = cmd.trigger,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = cmd.prompt,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            if (!cmd.isBuiltIn) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            commandManager.removeCustomCommand(cmd.trigger)
                                            commands = commandManager.getCommands()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.commands_delete_command),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { isSheetOpen = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
                    .imePadding()
            ) {
                Text(
                    text = if (editingTrigger != null) stringResource(R.string.commands_edit_command) else stringResource(R.string.commands_add_custom_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (editingTrigger == null) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = formSelectedType == CommandType.AI,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                formSelectedType = CommandType.AI
                                errorMessage = null
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                        ) {
                            Text(stringResource(R.string.commands_type_ai), fontSize = 12.sp)
                        }
                        SegmentedButton(
                            selected = formSelectedType == CommandType.TEXT_REPLACER,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                formSelectedType = CommandType.TEXT_REPLACER
                                errorMessage = null
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                        ) {
                            Text(stringResource(R.string.commands_type_replacer), fontSize = 12.sp)
                        }
                        SegmentedButton(
                            selected = formSelectedType == CommandType.FILE_SHARE,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                formSelectedType = CommandType.FILE_SHARE
                                errorMessage = null
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                        ) {
                            Text(stringResource(R.string.commands_type_file), fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = trigger,
                    onValueChange = {
                        trigger = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.commands_trigger_label, activePrefix)) },
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        label = {
                            Text(when (formSelectedType) {
                                CommandType.AI -> stringResource(R.string.commands_prompt_label)
                                CommandType.TEXT_REPLACER -> stringResource(R.string.commands_replacement_label)
                                CommandType.FILE_SHARE -> stringResource(R.string.commands_file_prompt_placeholder)
                            })
                        },
                        modifier = Modifier.weight(1f).height(120.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    if (formSelectedType == CommandType.FILE_SHARE) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { filePickerLauncher.launch("*/*") }) {
                            Text(stringResource(R.string.commands_browse))
                        }
                    }
                }

                errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { isSheetOpen = false }) {
                        Text(stringResource(R.string.commands_cancel))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            val trimmedTrigger = trigger.trim()
                            if (trimmedTrigger.isNotBlank() && prompt.isNotBlank()) {
                                if (!trimmedTrigger.startsWith(activePrefix)) {
                                    errorMessage = errorPrefixMsg
                                    return@Button
                                }
                                if (trimmedTrigger.length <= activePrefix.length || trimmedTrigger.substring(activePrefix.length).trim().isEmpty()) {
                                    errorMessage = errorEmptyMsg
                                    return@Button
                                }
                                if (commands.any { it.trigger == trimmedTrigger && it.trigger != editingTrigger }) {
                                    errorMessage = errorDuplicateMsg
                                    return@Button
                                }
                                if (formSelectedType == CommandType.FILE_SHARE && !checkFileExists(prompt.trim())) {
                                    errorMessage = context.getString(R.string.commands_error_file_missing)
                                    return@Button
                                }
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (editingTrigger != null) {
                                    commandManager.removeCustomCommand(editingTrigger!!)
                                }
                                val newCommand = Command(trimmedTrigger, prompt.trim(), false, formSelectedType)
                                commandManager.addCustomCommand(newCommand)
                                commands = commandManager.getCommands()
                                isSheetOpen = false
                            }
                        },
                        enabled = trigger.isNotBlank() && prompt.isNotBlank()
                    ) {
                        Text(if (editingTrigger != null) stringResource(R.string.commands_save_command) else stringResource(R.string.commands_add_command))
                    }
                }
            }
        }
    }
}
