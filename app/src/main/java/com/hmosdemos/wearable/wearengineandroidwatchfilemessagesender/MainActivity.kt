package com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.HealthKitClient
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.managers.AuthManager
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.managers.DeviceManager
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.managers.P2pManager
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.ui.WearEngineApp
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.ui.theme.WearEngineAndroidLiteFileMessageSenderTheme
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.viewmodels.MainViewModel
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.viewmodels.MainViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var deviceManager: DeviceManager
    private lateinit var authManager: AuthManager
    private lateinit var p2pManager: P2pManager
    private lateinit var healthKitClient: HealthKitClient
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        deviceManager = DeviceManager(this)
        authManager = AuthManager(this)
        p2pManager = P2pManager(this)
        healthKitClient = HealthKitClient(this)

        // Built with ViewModelProvider, not the viewModel() composable, so that the deep
        // link callbacks below reach the same instance the screen observes.
        viewModel = ViewModelProvider(
            this,
            MainViewModelFactory(
                authManager = authManager,
                deviceManager = deviceManager,
                p2pManager = p2pManager,
                healthKitClient = healthKitClient,
            ),
        )[MainViewModel::class.java]

        setContent {
            WearEngineAndroidLiteFileMessageSenderTheme {
                WearEngineApp(viewModel = viewModel)
            }
        }

        // The app can also be started cold by the deep link.
        intent?.data?.let(viewModel::handleHealthKitRedirect)
    }

    /**
     * Both authorization methods answer on this app's deep link. The launch mode of this
     * activity is singleTask, so the answer lands here instead of in a second instance.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let(viewModel::handleHealthKitRedirect)
    }

    @Deprecated("Kept because the login-free SDK reports launch errors this way.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.handleAuthorizationActivityResult(requestCode, data)
    }

    override fun onDestroy() {
        super.onDestroy()
        p2pManager.unregisterReceiver()
    }
}
