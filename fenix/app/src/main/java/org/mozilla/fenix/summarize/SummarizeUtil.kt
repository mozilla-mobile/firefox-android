package org.mozilla.fenix.summarize

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.*
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

private const val OPENAI_API_KEY = "API_KEY_HERE"
private const val OPENAI_API_MODEL = "gpt-3.5-turbo"

class SummarizeUtil {
    companion object {
        @OptIn(BetaOpenAI::class)
        fun getSummaryForUrl(url: String): Flow<String> {
            val openAI = OpenAI(token = OPENAI_API_KEY)

            val chatCompletionRequest = ChatCompletionRequest(
                model = ModelId(OPENAI_API_MODEL),
                messages = listOf(
                    ChatMessage(
                        role = ChatRole.User,
                        content = "Summarize $url",
                    ),
                ),
            )

            return openAI.chatCompletions(chatCompletionRequest).mapNotNull {
                it.choices[0].delta?.content
            }
        }
    }


}
