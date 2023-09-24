package com.flxrs.dankchat.data.api.emojis.dto

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class EmojisEmoteDto(
    val unified: String,
    @SerialName(value = "short_name") val shortName: String,
    val image: String,
)
