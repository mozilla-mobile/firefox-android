/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.messaging.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
import mozilla.components.service.nimbus.messaging.Message
import mozilla.components.service.nimbus.messaging.NimbusMessagingControllerInterface
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.appstate.AppAction.MessagingAction.ConsumeMessageToShow
import org.mozilla.fenix.components.appstate.AppAction.MessagingAction.Evaluate
import org.mozilla.fenix.components.appstate.AppAction.MessagingAction.MessageClicked
import org.mozilla.fenix.components.appstate.AppAction.MessagingAction.MessageDismissed
import org.mozilla.fenix.components.appstate.AppAction.MessagingAction.Restore
import org.mozilla.fenix.components.appstate.AppAction.MessagingAction.UpdateMessageToShow
import org.mozilla.fenix.components.appstate.AppAction.MessagingAction.UpdateMessages
import org.mozilla.fenix.components.appstate.AppState

typealias AppStoreMiddlewareContext = MiddlewareContext<AppState, AppAction>

class MessagingMiddleware(
    private val controller: NimbusMessagingControllerInterface,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) : Middleware<AppState, AppAction> {

    override fun invoke(
        context: AppStoreMiddlewareContext,
        next: (AppAction) -> Unit,
        action: AppAction,
    ) {
        when (action) {
            is Restore -> {
                coroutineScope.launch {
                    val messages = controller.getMessages()
                    context.store.dispatch(UpdateMessages(messages))
                }
            }

            is Evaluate -> {
                val message = controller.getNextMessage(
                    action.surface,
                    context.state.messaging.messages,
                )
                if (message != null) {
                    context.store.dispatch(UpdateMessageToShow(message))
                    onMessagedDisplayed(message, context)
                } else {
                    context.store.dispatch(ConsumeMessageToShow(action.surface))
                }
            }

            is MessageClicked -> onMessageClicked(action.message, context)

            is MessageDismissed -> onMessageDismissed(context, action.message)

            else -> {
                // no-op
            }
        }
        next(action)
    }

    private fun onMessagedDisplayed(
        oldMessage: Message,
        context: AppStoreMiddlewareContext,
    ) {
        coroutineScope.launch {
            val newMessage = controller.onMessageDisplayed(oldMessage)
            val newMessages = if (!newMessage.isExpired) {
                updateMessage(context, oldMessage, newMessage)
            } else {
                consumeMessageToShowIfNeeded(context, oldMessage)
                removeMessage(context, oldMessage)
            }
            context.store.dispatch(UpdateMessages(newMessages))
        }
    }

    private fun onMessageDismissed(
        context: AppStoreMiddlewareContext,
        message: Message,
    ) {
        val newMessages = removeMessage(context, message)
        context.store.dispatch(UpdateMessages(newMessages))
        consumeMessageToShowIfNeeded(context, message)
        coroutineScope.launch {
            controller.onMessageDismissed(message)
        }
    }

    private fun onMessageClicked(
        message: Message,
        context: AppStoreMiddlewareContext,
    ) {
        // Update Nimbus storage.
        coroutineScope.launch {
            controller.onMessageClicked(message)
        }
        // Update app state.
        val newMessages = removeMessage(context, message)
        context.store.dispatch(UpdateMessages(newMessages))
        consumeMessageToShowIfNeeded(context, message)
    }

    private fun consumeMessageToShowIfNeeded(
        context: AppStoreMiddlewareContext,
        message: Message,
    ) {
        val current = context.state.messaging.messageToShow[message.surface]
        if (current?.id == message.id) {
            context.store.dispatch(ConsumeMessageToShow(message.surface))
        }
    }

    private fun removeMessage(
        context: AppStoreMiddlewareContext,
        message: Message,
    ): List<Message> {
        return context.state.messaging.messages.filter { it.id != message.id }
    }

    private fun updateMessage(
        context: AppStoreMiddlewareContext,
        oldMessage: Message,
        updatedMessage: Message,
    ): List<Message> {
        val actualMessageToShow = context.state.messaging.messageToShow[updatedMessage.surface]

        if (actualMessageToShow?.id == oldMessage.id) {
            context.store.dispatch(UpdateMessageToShow(updatedMessage))
        }
        val oldMessageIndex = context.state.messaging.messages.indexOfFirst { it.id == updatedMessage.id }
        val newList = context.state.messaging.messages.toMutableList()
        newList[oldMessageIndex] = updatedMessage
        return newList
    }
}
