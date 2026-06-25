package io.axeptio.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.axeptio.sample.config.AppConfig
import io.axeptio.sample.config.AxeptioConfigManager
import io.axeptio.sample.ui.EnumDropdown
import io.axeptio.sdk.configuration.AxeptioEnvironment
import io.axeptio.sdk.configuration.AxeptioService
import io.axeptio.sdk.configuration.AxeptioThemeMode

@Composable
fun ConfigScreen(
    onConfigSaved: (AppConfig) -> Unit,
    initialConfig: AppConfig,
) {
    var formState by remember { mutableStateOf(ConfigFormState.from(initialConfig)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.config_title),
            style = MaterialTheme.typography.headlineSmall,
        )

        EnumDropdown(
            label = stringResource(R.string.config_label_target_service),
            selectedOption = formState.targetService,
            options = AxeptioService.entries,
            onOptionSelected = { selected ->
                formState = formState.copy(targetService = selected)
            }
        )

        EnumDropdown(
            label = stringResource(R.string.config_label_theme_mode),
            selectedOption = formState.themeMode,
            options = AxeptioThemeMode.entries,
            onOptionSelected = { formState = formState.copy(themeMode = it) },
        )

        OutlinedTextField(
            value = formState.projectId,
            onValueChange = { formState = formState.copy(projectId = it) },
            label = { Text(stringResource(R.string.config_label_project_id)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = formState.appVersion,
            onValueChange = { formState = formState.copy(appVersion = it) },
            label = { Text(stringResource(R.string.config_label_app_version)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = formState.token,
            onValueChange = { formState = formState.copy(token = it) },
            label = { Text(stringResource(R.string.config_label_token)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = formState.configId,
            onValueChange = { formState = formState.copy(configId = it) },
            label = { Text(stringResource(R.string.config_label_config_id)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        EnumDropdown(
            label = stringResource(R.string.config_label_environment),
            selectedOption = formState.environment,
            options = AxeptioEnvironment.entries,
            onOptionSelected = { formState = formState.copy(environment = it) }
        )

        Button(
            onClick = { onConfigSaved(formState.toAppConfig()) },
            modifier = Modifier.fillMaxWidth(),
            enabled = formState.projectId.isNotBlank() && formState.appVersion.isNotBlank()
        ) {
            Text(stringResource(R.string.config_btn_save))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfigScreenPreview() {
    ConfigScreen(
        onConfigSaved = {},
        initialConfig = AppConfig(
            projectId = AxeptioConfigManager.DEFAULT_PROJECT_ID,
            appVersion = AxeptioConfigManager.DEFAULT_APP_VERSION,
            targetService = AxeptioService.Brands,
            forceThemeMode = AxeptioThemeMode.Light,
            environment = AxeptioEnvironment.Staging,
        )
    )
}
