package com.bekindremind.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bekindremind.app.permissions.ExactAlarmPermissionManager
import com.bekindremind.app.ui.ExactAlarmBlockerScreen

class MainActivity : ComponentActivity() {
    private lateinit var exactAlarmPermissionManager: ExactAlarmPermissionManager
    private var canScheduleExactAlarms by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        exactAlarmPermissionManager = ExactAlarmPermissionManager(this)
        canScheduleExactAlarms = exactAlarmPermissionManager.canScheduleExactAlarms()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (canScheduleExactAlarms) {
                        HomeScreen()
                    } else {
                        ExactAlarmBlockerScreen(
                            onOpenSettings = {
                                startActivity(exactAlarmPermissionManager.createPermissionIntent())
                            },
                            onRefresh = {
                                canScheduleExactAlarms = exactAlarmPermissionManager.canScheduleExactAlarms()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::exactAlarmPermissionManager.isInitialized) {
            canScheduleExactAlarms = exactAlarmPermissionManager.canScheduleExactAlarms()
        }
    }
}

@Composable
private fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(id = R.string.app_name), style = MaterialTheme.typography.headlineSmall)
        Text(text = stringResource(id = R.string.app_subtitle), style = MaterialTheme.typography.bodyLarge)
    }
}
