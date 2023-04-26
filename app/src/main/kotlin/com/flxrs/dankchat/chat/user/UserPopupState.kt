package com.flxrs.dankchat.chat.user

import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName

sealed class UserPopupState {
    data class Loading(val userName: UserName, val displayName: DisplayName) : UserPopupState()
    data class Error(val throwable: Throwable? = null) : UserPopupState()
    data class Success(
        val userId: UserId,
        val userName: UserName,
        val displayName: DisplayName,
        val created: String,
        val avatarUrl: String,
        val showFollowingSince: Boolean = false,
        val followingSince: String? = null,
        val isBlocked: Boolean = false,
        val isSubscriptionHidden: Boolean,
        val isSubscribed: Boolean,
        val subscriptionTier: String,
        val subscribedMonths: Int
    ) : UserPopupState()

    data class NotLoggedIn(val userName: UserName, val displayName: DisplayName) : UserPopupState()
}
