package com.fit3161.fit3162.mogo.UIScreen.Profile

import android.net.Uri
import com.fit3161.fit3162.mogo.R
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.fit3161.fit3162.mogo.UIScreen.Profile.ProfileViewModel
import com.fit3161.fit3162.mogo.data.model.PresetDestinations
import kotlinx.coroutines.launch
/**
 * Profile screen UI
 *
 * Displays and allows editing of the authenticated user's profile information,
 * including their avatar, name, contact details, role, and home campus.
 */



/**
 * Root composable for the Profile screen.
 *
 * Shows:
 * - A tappable profile avatar with an edit icon that opens the device gallery
 * - Read-only fields for name, email, mobile, gender, role, and home campus,
 *   each with an edit icon that opens an inline dialog for updating the value
 * - A "Change Password" button (not yet implemented)
 * - A "Log Out" button
 *
 * Shows a loading indicator while profile data is being fetched, and an error
 * message if loading fails.
 *
 * @param viewModel The [ProfileViewModel] managing profile state and updates.
 * @param modifier Optional modifier applied to the root layout.
 * @param onLogout Callback invoked when the user confirms logout.
 * @param onRoleChanged Callback invoked after the user's role is successfully updated.
 * @param onNavigateToSettings Callback invoked when the user taps the settings icon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenUI(
    viewModel: ProfileViewModel,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit,
    onRoleChanged: () -> Unit = {},
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var userRole by remember { mutableStateOf("") }
    val homeCampus = uiState.homeCampus

    // Dialog Visibility Flags
    var showNameDialog by remember { mutableStateOf(false) }
    var showMobileDialog by remember { mutableStateOf(false) }
    var showGenderDialog by remember { mutableStateOf(false) }
    var showRoleDialog by remember { mutableStateOf(false) }
    var showCampusDialog by remember { mutableStateOf(false) }

    // Temporary Values for the edit dialogs (to avoid modifying live data until saved.)
    var tempName by remember { mutableStateOf("") }
    var tempMobile by remember { mutableStateOf("") }
    var tempGender by remember { mutableStateOf("") }
    var tempUserRole by remember { mutableStateOf("") }
    var tempHomeCampus by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState) {
        uiState.profile?.let {
            name = it.user_name
            email = it.user_email
            mobile = it.user_phone
            gender = it.user_gender
            userRole = it.user_role
        }
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (uiState.error != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: ${uiState.error}")
        }
        return
    }

    // Launcher for imaging picking
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            viewModel.updateProfilePicture(it)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ========== HEADER ROW (consistent with Offers screen) ==========
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Profile",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
                // No explicit color – uses default theme color (same as Offers title)
            )
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings"
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        // Profile picture
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0xFFDCCBFF))
                    .clickable { galleryLauncher.launch("image/*") }
            ) {
                val avatarUrl = uiState.profile?.avatar_url
                if (avatarUrl != null && selectedImageUri == null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Profile picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.p), fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Edit icon (pencil) at bottom‑right edge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-8).dp, y = (-8).dp)
                    .size(32.dp)
                    .background(Color.White, CircleShape)
                    .clickable { galleryLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                Text("✎", fontSize = 18.sp, color = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Name field
        OutlinedTextField(
            value = name,
            onValueChange = {},
            textStyle = TextStyle(color = Color.Black),
            label = { Text(stringResource(R.string.full_name)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            trailingIcon = {
                IconButton(onClick = {
                    tempName = name
                    showNameDialog = true
                }) {
                    Text("✎", fontSize = 20.sp)
                }
            }
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Email field
        OutlinedTextField(
            value = email,
            textStyle = TextStyle(color = Color.Black),
            onValueChange = {},
            label = { Text(stringResource(R.string.email)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = false
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Mobile field
        OutlinedTextField(
            value = mobile,
            textStyle = TextStyle(color = Color.Black),
            onValueChange = {},
            label = { Text(stringResource(R.string.mobile_number)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            trailingIcon = {
                IconButton(onClick = {
                    tempMobile = mobile
                    showMobileDialog = true
                }) {
                    Text("✎", fontSize = 20.sp)
                }
            }
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Gender field
        OutlinedTextField(
            value = gender,
            onValueChange = {},
            textStyle = TextStyle(color = Color.Black),
            label = { Text(stringResource(R.string.gender)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            trailingIcon = {
                IconButton(onClick = {
                    tempGender = gender
                    showGenderDialog = true
                }) {
                    Text("✎", fontSize = 20.sp)
                }
            }
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Role field
        OutlinedTextField(
            value = userRole,
            onValueChange = {},
            textStyle = TextStyle(color = Color.Black),
            label = { Text(stringResource(R.string.role_rider_driver)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            trailingIcon = {
                IconButton(onClick = {
                    tempUserRole = userRole
                    showRoleDialog = true
                }) {
                    Text("✎", fontSize = 20.sp)
                }
            }
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Home Campus
        OutlinedTextField(
            value = homeCampus?.name ?: "",
            onValueChange = {},
            textStyle = TextStyle(color = Color.Black),
            label = { Text(stringResource(R.string.home_campus)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            trailingIcon = {
                IconButton(onClick = {
                    tempHomeCampus = homeCampus?.name ?: ""
                    showCampusDialog = true
                }) {
                    Text("✎", fontSize = 20.sp)
                }
            }
        )

        Spacer(modifier = Modifier.height(80.dp))

        // Implement Change password functionality
        Button(
            onClick = { /* TODO: change password */ },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.change_password), color = Color.White)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Logs out the user and clears the session.
        // Navigation points back to the welcome screen.
        Button(
            onClick = { scope.launch { onLogout() } },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCA3433)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.log_out))
        }
    }

    // Name edit dialog
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text(stringResource(R.string.edit_full_name)) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text(stringResource(R.string.full_name)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempName.isNotBlank()) {
                            viewModel.updateField("name", tempName)
                        }
                        showNameDialog = false
                    }
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // Mobile edit dialog
    if (showMobileDialog) {
        AlertDialog(
            onDismissRequest = { showMobileDialog = false },
            title = { Text(stringResource(R.string.edit_mobile_number)) },
            text = {
                OutlinedTextField(
                    value = tempMobile,
                    onValueChange = { tempMobile = it },
                    label = { Text(stringResource(R.string.mobile_number)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempMobile.isNotBlank()) {
                            viewModel.updateField("mobile", tempMobile)
                        }
                        showMobileDialog = false
                    }
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showMobileDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // Gender edit dialog
    if (showGenderDialog) {
        val genderOptions = listOf("Female", "Male", "Non-binary", "Genderqueer", "Agender", "Transgender", "Prefer not to say", "Other")
        var expanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showGenderDialog = false },
            title = { Text(stringResource(R.string.edit_gender)) },
            text = {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = tempGender,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.gender)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        genderOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    tempGender = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempGender.isNotBlank()) {
                            viewModel.updateField("gender", tempGender)
                        }
                        showGenderDialog = false
                    }
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showGenderDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // Role edit dialog
    if (showRoleDialog) {
        val roleOptions = listOf("rider", "driver")
        AlertDialog(
            onDismissRequest = { showRoleDialog = false },
            title = { Text(stringResource(R.string.edit_role)) },
            text = {
                Column {
                    roleOptions.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tempUserRole = option }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = tempUserRole == option,
                                onClick = { tempUserRole = option }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(option)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempUserRole.isNotBlank()) {
                            viewModel.updateField("user_role", tempUserRole, onRoleChanged)
                        }
                        showRoleDialog = false
                    }
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showRoleDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // Home Campus edit dialog
    if (showCampusDialog) {
        var expanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showCampusDialog = false },
            title = { Text(stringResource(R.string.select_home_campus)) },
            text = {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = tempHomeCampus,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.campus)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        PresetDestinations.all.map { it.name }.forEach { option ->
                        DropdownMenuItem(
                                text = { Text(option) },
                                onClick = { tempHomeCampus = option; expanded = false }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (tempHomeCampus.isNotBlank()) {
                        viewModel.updateField("home_campus", tempHomeCampus)
                    }
                    showCampusDialog = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showCampusDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}