package io.axeptio.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.axeptio.sample.theme.SampleAppTheme

/**
 * The privacy policy Android 16 opens from the health permission settings: an app requesting
 * `android.permission.health.READ_HEART_RATE` (what `AxeptioPermission.BodySensors` asks for there)
 * must declare one. A real app shows the privacy policy it declares on Google Play.
 */
class HealthPermissionsRationaleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SampleAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = stringResource(R.string.health_permissions_rationale),
                        modifier = Modifier
                            .safeDrawingPadding()
                            .padding(24.dp),
                    )
                }
            }
        }
    }
}
