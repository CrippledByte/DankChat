package com.flxrs.dankchat.data.api

import io.ktor.client.*
import io.ktor.client.request.*

class IvrApiService(private val ktorClient: HttpClient) {
    suspend fun getSubage(channel: String, user: String) = ktorClient.get("twitch/subage/$user/$channel")
}