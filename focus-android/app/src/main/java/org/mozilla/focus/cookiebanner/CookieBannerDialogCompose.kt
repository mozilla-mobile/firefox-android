/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.cookiebanner

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.mozilla.focus.R
import org.mozilla.focus.ui.theme.FocusTheme
import org.mozilla.focus.ui.theme.focusColors
import org.mozilla.focus.ui.theme.focusTypography

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun CookieBannerDialogComposePreview() {
    FocusTheme {
        CookieBannerDialogCompose(
            dialogTitle = "Too many cookie requests?",
            dialogText =
            "Allow Focus to accept all cookie requests if a reject all option isn’t available? " +
                "This will dismiss even more cookie banners.",
            onAllowButtonClicked = {
            },
            onDeclineButtonClicked = {
            },
            allowButtonText = "ALLOW",
            declineButtonText = "DECLINE",
        )
    }
}

/**
 * Displays the cookie banner reducer dialog
 */
@Suppress("LongParameterList")
@Composable
fun CookieBannerDialogCompose(
    dialogTitle: String = "",
    dialogText: String = "",
    onAllowButtonClicked: () -> Unit,
    onDeclineButtonClicked: () -> Unit,
    allowButtonText: String = "null",
    declineButtonText: String = "null",
) {
    Dialog(
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false),
        onDismissRequest = onDeclineButtonClicked,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(color = focusColors.secondary),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    DialogTitle(
                        modifier = Modifier.padding(
                            top = 24.dp,
                            start = 24.dp,
                            end = 24.dp,
                            bottom = 8.dp,
                        ),
                        text = dialogTitle,
                    )
                    CloseButton(onCloseButtonClick = onDeclineButtonClicked)
                }
                DialogText(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp),
                    text = dialogText,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 24.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 8.dp,
                        alignment = Alignment.End,
                    ),
                ) {
                    DialogTextButton(declineButtonText, Modifier, onClick = onDeclineButtonClicked)
                    DialogTextButton(allowButtonText, Modifier, onClick = onAllowButtonClicked)
                }
            }
        }
    }
}

/**
 * Reusable composable for a dialog button with text.
 */
@Composable
fun DialogTextButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
    ) {
        Text(
            modifier = modifier,
            color = colorResource(
                R.color.cfr_pop_up_shape_end_color,
            ),
            text = text,
            fontSize = 14.sp,
            style = MaterialTheme.typography.button,
        )
    }
}

@Composable
private fun CloseButton(onCloseButtonClick: () -> Unit) {
    IconButton(
        modifier = Modifier
            .size(48.dp),
        onClick = onCloseButtonClick,
    ) {
        Icon(
            painter = painterResource(R.drawable.mozac_ic_close),
            contentDescription = stringResource(R.string.onboarding_close_button_content_description),
            tint = focusColors.closeIcon,
        )
    }
}

/**
 * Reusable composable for a dialog text.
 */
@Composable
fun DialogText(
    modifier: Modifier = Modifier,
    text: String,
) {
    Text(
        modifier = modifier,
        color = focusColors.onPrimary,
        text = text,
        style = focusTypography.cfrTextStyle,
    )
}

/**
 * Reusable composable for a dialog title.
 */
@Composable
fun DialogTitle(
    modifier: Modifier = Modifier,
    text: String,
) {
    Text(
        modifier = modifier,
        color = focusColors.onPrimary,
        fontWeight = FontWeight.Bold,
        text = text,
        style = focusTypography.cfrTextStyle,
    )
}
