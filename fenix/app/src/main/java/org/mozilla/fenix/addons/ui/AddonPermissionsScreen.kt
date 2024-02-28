/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.addons.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.LinkText
import org.mozilla.fenix.compose.LinkTextState
import org.mozilla.fenix.theme.FirefoxTheme

private const val LEARN_MORE_URL =
    "https://support.mozilla.org/kb/permission-request-messages-firefox-extensions"

/**
 * We create the Addon permissions screen by adding each section
 */
@Composable
fun AddonPermissionsScreen(
    permissions: List<String>,
    optionalPermissions: List<String>,
    originPermissions: List<String>,
) {
    val state = LinkTextState(
        text = stringResource(R.string.mozac_feature_addons_learn_more),
        url = LEARN_MORE_URL,
        onClick = {},
    )

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        RequiredSection(permissions)

        if (optionalPermissions.isNotEmpty()) {
            OptionalSection(optionalPermissions)
        }

        if (originPermissions.isNotEmpty()) {
            OriginsSection(originPermissions)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            LinkText(
                text = stringResource(R.string.mozac_feature_addons_learn_more),
                linkTextStates = listOf(state),
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark theme")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, name = "Light theme")
fun AddonPermissionsScreenPreview() {
    val permissions: List<String> = listOf("Permission required 1", "Permission required 2")
    val optionalPermissions: List<String> = listOf("Optional permission 1")
    val websitesWithPermissions: List<String> = listOf(
        "https://required.website",
        "https://optional-suggested.website...",
        "https://user-added.website.com",
    )

    FirefoxTheme {
        AddonPermissionsScreen(permissions, optionalPermissions, websitesWithPermissions)
    }
}
