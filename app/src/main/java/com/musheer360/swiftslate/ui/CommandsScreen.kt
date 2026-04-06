package com.musheer360.swiftslate.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musheer360.swiftslate.R
import com.musheer360.swiftslate.manager.CommandManager
import com.musheer360.swiftslate.model.Command
import com.musheer360.swiftslate.model.CommandType
import com.musheer360.swiftslate.ui.components.ScreenTitle
import com.musheer360.swiftslate.ui.components.SlateCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandsScreen() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val commandManager = remember { CommandManager(context) }
    var commands by remember { mutableStateOf(commandManager.getCommands()) }
    var trigger by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf(CommandType.AI) }
    var editingTrigger by remember { mutableStateOf<String?>(null) }
    val aiPrefix = commandManager.getTriggerPrefix()
    val fileSharePrefix = commandManager.getFileSharePrefix()
    val textReplacerPrefix = commandManager.getTextReplacerPrefix()
    val activePrefix = when (selectedType) {
        CommandType.AI -> aiPrefix
        CommandType.TEXT_REPLACER -> textReplacerPrefix
        CommandType.FILE_SHARE -> fileSharePrefix
    }
    val errorPrefixMsg = stringResource(R.string.commands_error_prefix, activePrefix)
    val errorDuplicateMsg = stringResource(R.string.commands_error_duplicate)

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(it, takeFlags)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp)
    ) {
        ScreenTitle(stringResource(R.string.commands_title))

        SlateCard {
            Text(
                text = stringResource(R.string.commands_add_custom_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
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
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { 
                        Text(when (selectedType) {
                            CommandType.AI -> stringResource(R.string.commands_prompt_label)
                            CommandType.TEXT_REPLACER -> stringResource(R.string.commands_replacement_label)
                            CommandType.FILE_SHARE -> stringResource(R.string.commands_file_prompt_placeholder)
                        }) 
                    },
                    modifier = Modifier.weight(1f).height(100.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                if (selectedType == CommandType.FILE_SHARE) {
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
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (editingTrigger != null) {
                    TextButton(
                        onClick = {
                            trigger = ""
                            prompt = ""
                            errorMessage = null
                            editingTrigger = null
                            selectedType = CommandType.AI
                        }
                    ) {
                        Text(stringResource(R.string.commands_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Button(
                    onClick = {
                        val trimmedTrigger = trigger.trim()
                        if (trimmedTrigger.isNotBlank() && prompt.isNotBlank()) {
                            if (!trimmedTrigger.startsWith(activePrefix)) {
                                errorMessage = errorPrefixMsg
                                return@Button
                            }
                            if (commands.any { it.trigger == trimmedTrigger && it.trigger != editingTrigger }) {
                                errorMessage = errorDuplicateMsg
                                return@Button
                            }
                            if (selectedType == CommandType.FILE_SHARE && !checkFileExists(prompt.trim())) {
                                errorMessage = context.getString(R.string.commands_error_file_missing)
                                return@Button
                            }
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (editingTrigger != null) {
                                commandManager.removeCustomCommand(editingTrigger!!)
                            }
                            val newCommand = Command(trimmedTrigger, prompt.trim(), false, selectedType)
                            commandManager.addCustomCommand(newCommand)
                            commands = commandManager.getCommands()
                            trigger = ""
                            prompt = ""
                            errorMessage = null
                            editingTrigger = null
                            selectedType = CommandType.AI
                        }
                    },
                    enabled = trigger.isNotBlank() && prompt.isNotBlank()
                ) {
                    Text(if (editingTrigger != null) stringResource(R.string.commands_save_command) else stringResource(R.string.commands_add_command))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(commands) { cmd ->
                SlateCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cmd.trigger,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (cmd.type == CommandType.TEXT_REPLACER) {
                                Text(
                                    text = stringResource(R.string.commands_type_replacer),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            } else if (cmd.type == CommandType.FILE_SHARE) {
                                Text(
                                    text = stringResource(R.string.commands_type_file),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            Text(
                                text = cmd.prompt,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (cmd.isBuiltIn) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.commands_built_in),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                        if (!cmd.isBuiltIn) {
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                trigger = cmd.trigger
                                prompt = cmd.prompt
                                selectedType = cmd.type
                                editingTrigger = cmd.trigger
                                errorMessage = null
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.commands_edit_command),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                commandManager.removeCustomCommand(cmd.trigger)
                                if (editingTrigger == cmd.trigger) {
                                    trigger = ""
                                    prompt = ""
                                    errorMessage = null
                                    editingTrigger = null
                                    selectedType = CommandType.AI
                                }
                                commands = commandManager.getCommands()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.commands_delete_command),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}