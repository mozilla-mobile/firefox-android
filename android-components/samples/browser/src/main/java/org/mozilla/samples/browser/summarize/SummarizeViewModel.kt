package org.mozilla.samples.browser.summarize

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.concept.fetch.Request
import mozilla.components.concept.fetch.isSuccess
import mozilla.components.lib.fetch.httpurlconnection.HttpURLConnectionClient
import org.json.JSONObject

class SummarizeViewModel : ViewModel() {
    suspend fun summarizeText(text: String): String = withContext(Dispatchers.IO) {

        val escapedText = escapeJsonString(text)

        val reqString = """
        {
            "model": "llama3",
            "messages": [
                {
                    "role": "user",
                    "content": "Could you summarize the below article for me? --- $escapedText ---"
                }
            ]
        }
    """.trimIndent()

        val fetchClient = HttpURLConnectionClient()

        val request = Request(
            url = "http://localhost:11434/api/chat",
            method = Request.Method.POST,
            body = Request.Body.fromString(reqString),
        )

        val response = fetchClient.fetch(request)
        if (!response.isSuccess) {
            response.close()
            return@withContext "Error!!! Please retry later."
        }

        val responseString = response.use { it.body.string() }

        return@withContext extractContentFromResponse(responseString)
    }

    private fun extractContentFromResponse(jsonResponse: String): String {
        val contentBuilder = StringBuilder()

        // Split the jsonResponse string into individual lines
        val responses = jsonResponse.split("\n")

        for (response in responses) {
            // Ignore empty lines
            if (response.isNotBlank()) {
                val jsonObject = JSONObject(response)
                if (jsonObject.has("message")) {
                    val messageObject = jsonObject.getJSONObject("message")
                    if (messageObject.has("content")) {
                        contentBuilder.append(messageObject.getString("content"))
                    }
                }
            }
        }

        return contentBuilder.toString()
    }

    fun escapeJsonString(input: String): String {
        val sb = StringBuilder()
        for (c in input) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '/' -> sb.append("\\/")
                else -> {
                    if (c.code in 0x0000..0x001F || c.code in 0x007F..0x009F || c.code in 0x2000..0x20FF) {
                        val hex = String.format("\\u%04x", c.code)
                        sb.append(hex)
                    } else {
                        sb.append(c)
                    }
                }
            }
        }
        return sb.toString()
    }
}