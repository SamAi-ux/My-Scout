package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.components.BentoCard
import com.example.ui.theme.CokelatSeragam
import com.example.ui.theme.CokelatTua
import com.example.ui.theme.KremHangat
import com.example.ui.theme.MerahLencana
import com.example.ui.theme.MerahLencanaSoft
import com.example.ui.theme.OutlineWarm
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AuthScreen(
    authError: String?,
    onGoogleSuccess: (uid: String, email: String, displayName: String) -> Unit,
    onAuthError: (String) -> Unit,
    onManualRegister: (fullName: String, username: String, email: String, password: String) -> Unit,
    onManualLogin: (identifier: String, password: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isRegisterMode by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }

    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Attempt silent Google auto-sign-in if already authorized on device
    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { uid, mail, name ->
                onGoogleSuccess(uid, mail, name)
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KremHangat)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Editorial Header & Hero Art
            BentoCard(containerColor = Color.White) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_scout_hero_1795584338572),
                        contentDescription = "Ilustrasi My Scout",
                        modifier = Modifier
                            .size(74.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "My Scout",
                            style = MaterialTheme.typography.displaySmall,
                            color = CokelatTua
                        )
                        Text(
                            text = "Platform Kepramukaan Modern • Gugus Depan Digital",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!authError.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MerahLencanaSoft,
                    border = BorderStroke(1.dp, MerahLencana.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = authError,
                        color = MerahLencana,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // Path 1: Google Sign-In / Sign-Up & Drive JSON Sync
            BentoCard {
                Text(
                    text = "Daftar / Masuk lewat Google",
                    style = MaterialTheme.typography.titleMedium,
                    color = CokelatTua
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Sinkronisasi otomatis cloud & cadangan JSON Google Drive di latar belakang.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        isGoogleLoading = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            scope = scope,
                            onSuccess = { uid, mail, name ->
                                isGoogleLoading = false
                                onGoogleSuccess(uid, mail, name)
                            },
                            onError = { msg ->
                                isGoogleLoading = false
                                onAuthError(msg)
                            },
                            onCancelled = {
                                isGoogleLoading = false
                            }
                        )
                    },
                    enabled = !isGoogleLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("google_signin_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CokelatTua,
                        contentColor = Color.White
                    )
                ) {
                    if (isGoogleLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = "Google Sign-In",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Lanjutkan dengan Google", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = OutlineWarm)
                Text(
                    text = "  ATAU PENDAFTARAN / MASUK MANUAL  ",
                    style = MaterialTheme.typography.labelSmall,
                    color = CokelatSeragam
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = OutlineWarm)
            }

            // Path 2: Manual Registration / Login (Hashed Password)
            BentoCard {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !isRegisterMode,
                        onClick = { isRegisterMode = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("manual_login_tab")
                    ) {
                        Text("Masuk Akun")
                    }
                    SegmentedButton(
                        selected = isRegisterMode,
                        onClick = { isRegisterMode = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("manual_register_tab")
                    ) {
                        Text("Daftar Baru")
                    }
                }

                Spacer(Modifier.height(14.dp))

                if (isRegisterMode) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Nama Lengkap") },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_fullname_input")
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        leadingIcon = { Icon(Icons.Filled.AccountCircle, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_username_input")
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_email_input")
                    )
                } else {
                    OutlinedTextField(
                        value = identifier,
                        onValueChange = { identifier = it },
                        label = { Text("Username atau Email") },
                        leadingIcon = { Icon(Icons.Filled.AccountCircle, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_identifier_input")
                    )
                }

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (min. 6 karakter)") },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_password_input")
                )

                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = {
                        if (isRegisterMode) {
                            onManualRegister(fullName, username, email, password)
                        } else {
                            onManualLogin(identifier, password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("manual_submit_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, CokelatTua)
                ) {
                    Text(
                        text = if (isRegisterMode) "Daftar Akun Baru" else "Masuk ke My Scout",
                        fontWeight = FontWeight.SemiBold,
                        color = CokelatTua
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Pemilihan jenjang Pramuka hanya tampil sekali setelah pendaftaran baru & data tersimpan otomatis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: (uid: String, email: String, displayName: String) -> Unit
) {
    val existing = Firebase.auth.currentUser
    if (existing != null) {
        onAuthSuccess(existing.uid, existing.email.orEmpty(), existing.displayName.orEmpty())
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    // Launched via caller's LaunchedEffect coroutine scope
    kotlinx.coroutines.MainScope().launch {
        try {
            val cred = credentialManager.getCredential(context, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleCred = GoogleIdTokenCredential.createFrom(cred.data)
                val authResult = Firebase.auth
                    .signInWithCredential(GoogleAuthProvider.getCredential(googleCred.idToken, null))
                    .await()
                val user = authResult.user
                if (user != null) {
                    onAuthSuccess(
                        user.uid,
                        user.email.orEmpty(),
                        user.displayName ?: googleCred.displayName.orEmpty()
                    )
                }
            }
        } catch (_: Exception) {
            // Silent auto sign-in fallback: keep interactive login screen visible
        }
    }
}

private fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSuccess: (uid: String, email: String, displayName: String) -> Unit,
    onError: (String) -> Unit,
    onCancelled: () -> Unit
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onError("Konfigurasi Google Sign-In belum tersedia.")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activity = context as? Activity
                ?: throw IllegalStateException("Context bukan Activity.")
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleCred = GoogleIdTokenCredential.createFrom(cred.data)
                val authResult = Firebase.auth
                    .signInWithCredential(GoogleAuthProvider.getCredential(googleCred.idToken, null))
                    .await()
                val user = authResult.user
                if (user != null) {
                    onSuccess(
                        user.uid,
                        user.email.orEmpty(),
                        user.displayName ?: googleCred.displayName.orEmpty()
                    )
                } else {
                    onError("Gagal mendapatkan sesi Google.")
                }
            } else {
                onError("Tipe kredensial tidak dikenali.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onError(e.localizedMessage ?: "Login Google gagal. Pastikan akun Google aktif di perangkat.")
        }
    }
}
