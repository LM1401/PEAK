package com.example.peak.ui.screens.addons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.example.peak.domain.model.Addon

/**
 * Android TV UI Screen for managing installed Stremio add-ons.
 * Supports viewing, installing via manifest URL, retrying failed manifests, toggling, and removing.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AddonManagementScreen(
    viewModel: AddonViewModel,
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(48.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Add-ons",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Manage installed Stremio add-ons and stream providers",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { viewModel.showAddDialog(true) },
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0xFF00D2FF),
                        focusedContainerColor = Color.White,
                        contentColor = Color(0xFF040B16),
                        focusedContentColor = Color(0xFF040B16)
                    ),
                    shape = ButtonDefaults.shape(RoundedCornerShape(20.dp)),
                    modifier = Modifier.height(40.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Add-on", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // Error / Success Banners
            if (uiState.errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .background(Color(0xFF8B0000).copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearMessages() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White)
                        }
                    }
                }
            } else if (uiState.successMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .background(Color(0xFF006400).copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.successMessage ?: "",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearMessages() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White)
                        }
                    }
                }
            }

            // Installed Add-ons List or Empty State
            if (uiState.installedAddons.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No add-ons installed.",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Click 'Add Add-on' above to enter a Stremio manifest URL.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.installedAddons, key = { it.id }) { addon ->
                        AddonRowCard(
                            addon = addon,
                            isRetrying = uiState.retryingAddonId == addon.id,
                            onToggle = { enabled -> viewModel.toggleAddon(addon.id, enabled) },
                            onRetry = { viewModel.retryManifest(addon.id) },
                            onDelete = { viewModel.showDeleteConfirm(addon) }
                        )
                    }
                }
            }
        }

        // DIALOG OVERLAYS
        if (uiState.showAddDialog) {
            InstallAddonDialog(
                isLoading = uiState.isLoading,
                errorMessage = uiState.errorMessage,
                onConfirm = { url -> viewModel.installAddon(url) },
                onDismiss = { viewModel.showAddDialog(false) }
            )
        }

        uiState.addonToDelete?.let { addon ->
            ConfirmDeleteAddonDialog(
                addon = addon,
                onConfirm = { viewModel.deleteAddon(addon.id) },
                onDismiss = { viewModel.showDeleteConfirm(null) }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AddonRowCard(
    addon: Addon,
    isRetrying: Boolean,
    onToggle: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = { onToggle(!addon.isEnabled) },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF0F1B2D),
            focusedContainerColor = Color(0xFF1B2E4A)
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color(0xFF00D2FF)),
                shape = RoundedCornerShape(12.dp)
            )
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = addon.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    addon.version?.let { ver ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = "v$ver", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Manifest Status Badge
                    if (addon.manifest != null) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF006400).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(text = "Active", fontSize = 11.sp, color = Color(0xFF90EE90), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF8B0000).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(text = "Manifest Pending / Failed", fontSize = 11.sp, color = Color(0xFFFFA07A), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = addon.description ?: addon.transportUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (addon.manifest == null) {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.colors(
                            containerColor = Color.White.copy(alpha = 0.12f),
                            focusedContainerColor = Color.White,
                            contentColor = Color.White,
                            focusedContentColor = Color.Black
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isRetrying) "Retrying..." else "Retry", fontSize = 12.sp)
                        }
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove add-on",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun InstallAddonDialog(
    isLoading: Boolean,
    errorMessage: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var urlText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(520.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = SurfaceDefaults.colors(containerColor = Color(0xFF0A1322))
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Install Stremio Add-on",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter a manifest URL (e.g. https://torrentio.strem.fun/manifest.json)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(Color(0xFF142238), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF00D2FF).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (urlText.isEmpty()) {
                        Text(
                            text = "https://...",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 15.sp
                        )
                    }

                    BasicTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                        cursorBrush = SolidColor(Color(0xFF00D2FF)),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (urlText.isNotBlank() && !isLoading) onConfirm(urlText) }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFF6B6B),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.colors(
                            containerColor = Color.White.copy(alpha = 0.12f),
                            focusedContainerColor = Color.White,
                            contentColor = Color.White,
                            focusedContentColor = Color.Black
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("Cancel", fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = { if (urlText.isNotBlank() && !isLoading) onConfirm(urlText) },
                        enabled = urlText.isNotBlank() && !isLoading,
                        colors = ButtonDefaults.colors(
                            containerColor = Color(0xFF00D2FF),
                            focusedContainerColor = Color.White,
                            contentColor = Color(0xFF040B16),
                            focusedContentColor = Color(0xFF040B16)
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isLoading) {
                                Text("Installing...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Install", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ConfirmDeleteAddonDialog(
    addon: Addon,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(440.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = SurfaceDefaults.colors(containerColor = Color(0xFF0A1322))
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Remove Add-on",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Are you sure you want to remove '${addon.displayName}'?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.colors(
                            containerColor = Color.White.copy(alpha = 0.12f),
                            focusedContainerColor = Color.White,
                            contentColor = Color.White,
                            focusedContentColor = Color.Black
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("Cancel", fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.colors(
                            containerColor = Color(0xFFFF4D4D),
                            focusedContainerColor = Color.White,
                            contentColor = Color.White,
                            focusedContentColor = Color.Black
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("Remove", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
