package com.verifyetb.app.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.verifyetb.app.ui.theme.AccentGold
import com.verifyetb.app.ui.theme.OnGold
import com.verifyetb.app.ui.theme.BgPrimary
import com.verifyetb.app.ui.theme.TextMuted
import com.verifyetb.app.ui.theme.TextPrimary

@Composable
fun OnboardingScreen(
    onContinue: () -> Unit,
    onSmsGranted: () -> Unit = {},
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result.values.any { it }) onSmsGranted()
        onContinue()
    }

    fun requestOrContinue() {
        val receive = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECEIVE_SMS,
        ) == PackageManager.PERMISSION_GRANTED
        val read = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_SMS,
        ) == PackageManager.PERMISSION_GRANTED
        if (receive && read) {
            onSmsGranted()
            onContinue()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS,
                ),
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = "Verifyetb",
                color = AccentGold,
                style = MaterialTheme.typography.headlineLarge,
            )
            Box(modifier = Modifier.height(28.dp))
            Text(
                text = "Your spending, tracked from bank SMS.",
                color = TextPrimary,
                style = MaterialTheme.typography.headlineMedium,
            )
            Box(modifier = Modifier.height(16.dp))
            Text(
                text = "Verifyetb only reads Ethiopian bank and mobile-wallet transaction SMS on this phone. Nothing leaves the device — no account, no server.",
                color = TextMuted,
                style = MaterialTheme.typography.bodyLarge,
            )
            Box(modifier = Modifier.height(20.dp))
            Text(
                text = "Allow SMS access and Verifyetb reads the bank messages already on this phone. If you skip, you can still add spending by hand.",
                color = TextMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { requestOrContinue() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGold,
                    contentColor = OnGold,
                ),
            ) {
                Text(text = "Allow SMS access")
            }
            TextButton(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                Text(text = "Continue without SMS", color = TextMuted)
            }
            Box(modifier = Modifier.height(8.dp))
            Text(
                text = "You can change this later in Settings.",
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
