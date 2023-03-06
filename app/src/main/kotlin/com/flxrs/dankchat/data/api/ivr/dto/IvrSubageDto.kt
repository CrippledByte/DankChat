package com.flxrs.dankchat.data.api.ivr.dto

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class IvrSubageDto(
    @SerialName(value = "statusHidden") val hidden: Boolean,
    @SerialName(value = "meta") val meta: Meta?,
    @SerialName(value = "cumulative") val cumulative: Cumulative?
)

@Keep
@Serializable
data class Meta(
    @SerialName(value = "tier") val tier: String?
)

@Keep
@Serializable
data class Cumulative(
    @SerialName(value = "months") val months: Int?
)
