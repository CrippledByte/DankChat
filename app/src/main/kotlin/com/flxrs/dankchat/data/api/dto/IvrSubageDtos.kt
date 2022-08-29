package com.flxrs.dankchat.data.api.dto

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class IvrSubageDtos(
    @SerialName(value = "hidden") val hidden: Boolean,
    @SerialName(value = "subscribed") val subscribed: Boolean,
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
