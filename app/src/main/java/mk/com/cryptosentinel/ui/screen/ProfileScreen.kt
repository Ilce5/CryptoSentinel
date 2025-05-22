package mk.com.cryptosentinel.ui.screen

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val activity = context as Activity
    val auth = remember { FirebaseAuth.getInstance() }
    var user by remember { mutableStateOf(auth.currentUser) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.result
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener(activity) { taskResult ->
                    if (taskResult.isSuccessful) {
                        user = auth.currentUser
                    } else {
                        error = "Login failed: ${taskResult.exception?.message}"
                    }
                    isLoading = false
                }
        } catch (e: Exception) {
            error = "Google sign-in failed: ${e.localizedMessage}"
            isLoading = false
        }
    }

    fun launchGoogleSignIn() {
        isLoading = true
        error = null
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("187978495424-v1bguj5jh7016jtsapiku5aq320k5qa4.apps.googleusercontent.com") // <- From Firebase Console
            .requestEmail()
            .build()

        val client = GoogleSignIn.getClient(context, gso)
        launcher.launch(client.signInIntent)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (user != null) {
            Text("Welcome, ${user?.displayName}", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                auth.signOut()
                user = null
            }) {
                Text("Logout")
            }
        } else {
            Button(
                onClick = { launchGoogleSignIn() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE17C0D))
            ) {
                Text("Sign in with Google", color = Color.White)
            }
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }

        error?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
