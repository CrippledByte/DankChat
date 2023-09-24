package com.flxrs.dankchat.data.api.emojis

import com.flxrs.dankchat.data.api.emojis.dto.EmojisEmoteDto
import com.flxrs.dankchat.data.api.throwApiErrorOnFailure
import io.ktor.client.call.body
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmojisApiClient @Inject constructor(private val emojisApi: EmojisApi, private val json: Json) {

    suspend fun getEmojiEmotes(): Result<List<EmojisEmoteDto>> = runCatching {
        emojisApi.getEmojiEmotes()
            .throwApiErrorOnFailure(json)
            .body()
    }
}
