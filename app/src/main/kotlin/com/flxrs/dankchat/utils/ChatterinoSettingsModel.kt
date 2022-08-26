package com.flxrs.dankchat.utils

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChatterinoSettingsModel(
    val appearance: Appearance,
    val emotes: Emotes?,
    val highlighting: Highlighting,
    val misc: Misc
)

@JsonClass(generateAdapter = true)
data class Appearance(
    val badges: Badges,
    val messages: Messages
)

@JsonClass(generateAdapter = true)
data class Emotes(
    val showUnlistedEmotes: Boolean?,
    val enableGifAnimations: Boolean?
)

@JsonClass(generateAdapter = true)
data class Highlighting(
    val highlights: List<Highlight>,
    val users: List<User>
)

@JsonClass(generateAdapter = true)
data class Misc(
    val twitch: Twitch
)

@JsonClass(generateAdapter = true)
data class Badges(
    val vanity: Boolean,
    val ChannelAuthority: Boolean,
    val predictions: Boolean,
    val GlobalAuthority: Boolean,
    val subscription: Boolean?
)

@JsonClass(generateAdapter = true)
data class Messages(
    val alternateMessageBackground: Boolean,
    val hideModerated: Boolean,
    val showTimestamps: Boolean,
    val separateMessages: Boolean,
    val timestampFormat: String
)

@JsonClass(generateAdapter = true)
data class Highlight(
    val alert: Boolean,
    val pattern: String,
    val regex: Boolean
)

@JsonClass(generateAdapter = true)
data class User(
    val alert: Boolean,
    val pattern: String,
    val regex: Boolean
)

@JsonClass(generateAdapter = true)
data class Twitch(
    val messageHistoryLimit: Int,
    val loadMessageHistoryOnConnect: Boolean?
)