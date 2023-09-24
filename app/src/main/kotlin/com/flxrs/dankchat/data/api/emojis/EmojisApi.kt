package com.flxrs.dankchat.data.api.emojis

import io.ktor.client.HttpClient
import io.ktor.client.request.get

class EmojisApi(private val ktorClient: HttpClient) {

    suspend fun getEmojiEmotes() = ktorClient.get("emoji-data/emoji.json")
}
