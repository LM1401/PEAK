package com.example.peak.ui.screens.watchlist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
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
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Watchlist

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CreateListDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    errorMessage: String? = null
) {
    var text by remember { mutableStateOf("") }
    val inputFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        inputFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(480.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = SurfaceDefaults.colors(containerColor = Color(0xFF0A1322))
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Create New List",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Organize your favourite films and TV shows.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Text Input Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(Color(0xFF142238), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF00D2FF).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = "List name (e.g., Weekend Horror)",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 15.sp
                        )
                    }

                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(inputFocusRequester),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.White,
                            fontSize = 16.sp
                        ),
                        cursorBrush = SolidColor(Color(0xFF00D2FF)),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) onConfirm(text) })
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFF5252),
                        style = MaterialTheme.typography.labelMedium
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
                            containerColor = Color.White.copy(alpha = 0.1f),
                            focusedContainerColor = Color.White.copy(alpha = 0.25f),
                            contentColor = Color.White,
                            focusedContentColor = Color.White
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(20.dp)),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = { if (text.isNotBlank()) onConfirm(text) },
                        colors = ButtonDefaults.colors(
                            containerColor = Color(0xFF00D2FF),
                            focusedContainerColor = Color.White,
                            contentColor = Color(0xFF040B16),
                            focusedContentColor = Color(0xFF040B16)
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(20.dp)),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Text("Create List", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EditListDialog(
    watchlist: Watchlist,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    errorMessage: String? = null
) {
    var newName by remember { mutableStateOf(watchlist.name) }
    var isConfirmingDelete by remember { mutableStateOf(false) }
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
                .width(480.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = SurfaceDefaults.colors(containerColor = Color(0xFF0A1322))
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = if (isConfirmingDelete) "Delete List?" else "Manage '${watchlist.name}'",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isConfirmingDelete) {
                    Text(
                        text = "Are you sure you want to delete this list? Titles will remain saved in your other lists.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { isConfirmingDelete = false },
                            colors = ButtonDefaults.colors(
                                containerColor = Color.White.copy(alpha = 0.1f),
                                focusedContainerColor = Color.White,
                                contentColor = Color.White,
                                focusedContentColor = Color.Black
                            ),
                            shape = ButtonDefaults.shape(RoundedCornerShape(20.dp))
                        ) {
                            Text("Back")
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = onDelete,
                            colors = ButtonDefaults.colors(
                                containerColor = Color(0xFFFF5252),
                                focusedContainerColor = Color.White,
                                contentColor = Color.White,
                                focusedContentColor = Color(0xFFFF5252)
                            ),
                            shape = ButtonDefaults.shape(RoundedCornerShape(20.dp))
                        ) {
                            Text("Confirm Delete", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text(
                        text = "Rename or remove this custom list.",
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
                        BasicTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White,
                                fontSize = 16.sp
                            ),
                            cursorBrush = SolidColor(Color(0xFF00D2FF)),
                            singleLine = true
                        )
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            color = Color(0xFFFF5252),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!watchlist.isDefault) {
                            Button(
                                onClick = { isConfirmingDelete = true },
                                colors = ButtonDefaults.colors(
                                    containerColor = Color(0xFFFF5252).copy(alpha = 0.2f),
                                    focusedContainerColor = Color(0xFFFF5252),
                                    contentColor = Color(0xFFFF5252),
                                    focusedContentColor = Color.White
                                ),
                                shape = ButtonDefaults.shape(RoundedCornerShape(20.dp))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Delete List")
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        Row {
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.colors(
                                    containerColor = Color.White.copy(alpha = 0.1f),
                                    focusedContainerColor = Color.White,
                                    contentColor = Color.White,
                                    focusedContentColor = Color.Black
                                ),
                                shape = ButtonDefaults.shape(RoundedCornerShape(20.dp))
                            ) {
                                Text("Cancel")
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                onClick = { if (newName.isNotBlank()) onRename(newName) },
                                colors = ButtonDefaults.colors(
                                    containerColor = Color(0xFF00D2FF),
                                    focusedContainerColor = Color.White,
                                    contentColor = Color(0xFF040B16),
                                    focusedContentColor = Color(0xFF040B16)
                                ),
                                shape = ButtonDefaults.shape(RoundedCornerShape(20.dp))
                            ) {
                                Text("Save Changes", fontWeight = FontWeight.Bold)
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
fun RandomPickResultDialog(
    movie: Movie,
    listName: String,
    onViewDetails: (Movie) -> Unit,
    onPickAgain: () -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(520.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = SurfaceDefaults.colors(containerColor = Color(0xFF081220))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "RANDOM PICK • $listName".uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF00D2FF),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = movie.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (movie.year.isNotBlank()) {
                        Text(
                            text = movie.year,
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    if (movie.duration.isNotBlank()) {
                        Text(text = " • ", color = Color.White.copy(alpha = 0.4f))
                        Text(
                            text = movie.duration,
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    if (movie.genres.isNotBlank()) {
                        Text(text = " • ", color = Color.White.copy(alpha = 0.4f))
                        Text(
                            text = movie.genres.split(",").firstOrNull() ?: "",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = movie.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onViewDetails(movie) },
                        modifier = Modifier
                            .height(44.dp)
                            .focusRequester(focusRequester),
                        colors = ButtonDefaults.colors(
                            containerColor = Color(0xFF00D2FF),
                            focusedContainerColor = Color.White,
                            contentColor = Color(0xFF040B16),
                            focusedContentColor = Color(0xFF040B16)
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(22.dp))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View Details", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onPickAgain,
                        modifier = Modifier.height(44.dp),
                        colors = ButtonDefaults.colors(
                            containerColor = Color.White.copy(alpha = 0.12f),
                            focusedContainerColor = Color.White,
                            contentColor = Color.White,
                            focusedContentColor = Color.Black
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(22.dp))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick Again")
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.height(44.dp),
                        colors = ButtonDefaults.colors(
                            containerColor = Color.Transparent,
                            focusedContainerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White.copy(alpha = 0.6f),
                            focusedContentColor = Color.White
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(22.dp))
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
