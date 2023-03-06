package com.flxrs.dankchat.data.api.ivr

import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import io.ktor.client.HttpClient
import io.ktor.client.request.*

class IvrApi(private val ktorClient: HttpClient) {

    suspend fun getSubage(channel: UserName, userName: UserName) = ktorClient.get("v2/twitch/subage/$userName/$channel")

}
