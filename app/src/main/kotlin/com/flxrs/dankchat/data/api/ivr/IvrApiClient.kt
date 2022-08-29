package com.flxrs.dankchat.data.api.ivr

import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.api.ivr.dto.IvrSubageDto
import com.flxrs.dankchat.data.api.throwApiErrorOnFailure
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IvrApiClient @Inject constructor(private val ivrApi: IvrApi, private val json: Json) {

    suspend fun getSubage(channel: UserName, userName: UserName): Result<IvrSubageDto?> = runCatching {
        ivrApi.getSubage(channel, userName)
            .throwApiErrorOnFailure(json)
            .body()
    }
}
