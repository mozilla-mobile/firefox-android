/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.addons.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.SwitchWithLabel
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * Displays all of the addon's optional permissions
 *
 * @optionalPermissions the list of optional permissions
 */
@Composable
fun OptionalSection(optionalPermissions: List<String>) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.mozac_feature_addons_permissions_heading_optional),
                color = FirefoxTheme.colors.textAccent,
                style = FirefoxTheme.typography.headline8,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .semantics { heading() },
            )
        }

        LazyColumn() {
            items(optionalPermissions) { optionalPermission ->
                var enabledState by remember { mutableStateOf(false) }
                Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    SwitchWithLabel(
                        label = optionalPermission,
                        checked = enabledState,
                        onCheckedChange = { enabledState = it },
                    )
                }
            }
        }

        Divider()
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark theme")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, name = "Light theme")
fun SectionOptionalPreview() {
    val optionalPermissions: List<String> = listOf("Use your location", "Get data from clipboard")

    FirefoxTheme {
        OptionalSection(optionalPermissions)
    }
}
