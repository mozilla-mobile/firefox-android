/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.compose.cfr

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * UI for displaying a CFR in compose exclusive layouts when an anchor
 * cannot be passed as parameter
 *
 * @param alignment [Alignment] define the alignment of the CFR in relation to parent.
 * @param offset [IntOffset] define the offset of the Popup in relation to parent. Default (0,0)
 * @param properties [CFRPopupProperties] allowing to customize the popup appearance and behavior.
 * @param onDismiss Callback for when the popup is dismissed indicating also if the dismissal
 * was explicit - by tapping the "X" button or not.
 * @param text [Text] already styled and ready to be shown in the popup.
 * @param action Optional other composable to show just below the popup text.
 */
@Composable
@Suppress("MagicNumber")
fun CFRPopupForCompose(
    alignment: Alignment,
    offset: IntOffset = IntOffset(0, 0),
    properties: CFRPopupProperties,
    onDismiss: () -> Unit,
    text: @Composable (() -> Unit),
    action: @Composable (() -> Unit) = {},
) {
    var popupControl by remember { mutableStateOf(true) }

    if (popupControl) {
        Popup(
            alignment = alignment,
            offset = offset,
            properties = PopupProperties(
                focusable = properties.focusable,
                dismissOnBackPress = properties.dismissOnBackPress,
                dismissOnClickOutside = properties.dismissOnClickOutside,
            ),
        ) {
            CFRPopupContent(
                popupBodyColors = properties.popupBodyColors,
                showDismissButton = properties.showDismissButton,
                dismissButtonColor = properties.dismissButtonColor,
                indicatorDirection = properties.indicatorDirection,
                indicatorArrowStartOffset = with(LocalDensity.current) {
                    properties.indicatorArrowStartOffset
                },
                onDismiss = {
                    onDismiss()
                    popupControl = false
                },
                popupWidth = properties.popupWidth,
                text = text,
                action = action,
            )
        }
    }
}
