package com.flxrs.dankchat.chat.user

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.size.Scale
import com.flxrs.dankchat.R
import com.flxrs.dankchat.databinding.TimeoutDialogBinding
import com.flxrs.dankchat.databinding.UserPopupBottomsheetBinding
import com.flxrs.dankchat.main.MainFragment
import com.flxrs.dankchat.utils.extensions.collectFlow
import com.flxrs.dankchat.utils.extensions.isLandscape
import com.flxrs.dankchat.utils.extensions.loadImage
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserPopupDialogFragment : BottomSheetDialogFragment() {
    private val args: UserPopupDialogFragmentArgs by navArgs()
    private val viewModel: UserPopupViewModel by viewModels()
    private var bindingRef: UserPopupBottomsheetBinding? = null
    private val binding get() = bindingRef!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        bindingRef = UserPopupBottomsheetBinding.inflate(inflater, container, false).apply {
            userMention.text = when {
                args.isWhisperPopup -> getString(R.string.user_popup_whisper)
                else                -> getString(R.string.user_popup_mention)
            }

            userMention.setOnClickListener {
                val result = when {
                    args.isWhisperPopup -> UserPopupResult.Whisper(viewModel.userName)
                    else                -> UserPopupResult.Mention(viewModel.userName, viewModel.displayName)
                }

                findNavController()
                    .getBackStackEntry(R.id.mainFragment)
                    .savedStateHandle[MainFragment.USER_POPUP_RESULT_KEY] = result
                dialog?.dismiss()
            }

            userBlock.setOnClickListener {
                when {
                    viewModel.isBlocked -> viewModel.unblockUser()
                    else                -> MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.confirm_user_block_title)
                        .setMessage(R.string.confirm_user_block_message)
                        .setPositiveButton(R.string.confirm_user_block_positive_button) { _, _ -> viewModel.blockUser() }
                        .setNegativeButton(R.string.dialog_cancel) { d, _ -> d.dismiss() }
                        .show()
                }
            }
            userTimeout.setOnClickListener { showTimeoutDialog() }
            userDelete.setOnClickListener { showDeleteDialog() }
            userBan.setOnClickListener { showBanDialog() }
            userUnban.setOnClickListener {
                lifecycleScope.launch {
                    viewModel.unbanUser()
                    dialog?.dismiss()
                }
            }
            userAvatarCard.setOnClickListener {
                val userName = viewModel.userName
                val url = "https://twitch.tv/$userName"
                Intent(Intent.ACTION_VIEW).also {
                    it.data = url.toUri()
                    startActivity(it)
                }
            }
            userReport.setOnClickListener {
                val userName = viewModel.userName
                val url = "https://twitch.tv/$userName/report"
                Intent(Intent.ACTION_VIEW).also {
                    it.data = url.toUri()
                    startActivity(it)
                }
            }
            userBadges.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                val adapter = UserPopupBadgeAdapter().also { adapter = it }
                adapter.submitList(args.badges.toList())
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        collectFlow(viewModel.userPopupState) {
            when (it) {
                is UserPopupState.Loading     -> binding.showLoadingState(it)
                is UserPopupState.NotLoggedIn -> binding.showNotLoggedInState(it)
                is UserPopupState.Success     -> binding.updateUserData(it)
                is UserPopupState.Error       -> setErrorResultAndDismiss(it.throwable)
            }
        }

        collectFlow(viewModel.canShowModeration) {
            binding.moderationGroup.isVisible = it
        }

        dialog?.takeIf { isLandscape }?.let {
            val sheet = it as BottomSheetDialog
            sheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingRef = null
    }

    private fun UserPopupBottomsheetBinding.showLoadingState(state: UserPopupState.Loading) {
        userGroup.isVisible = false
        userLoading.isVisible = true
        userBlock.isEnabled = false
        userName.text = state.userName.formatWithDisplayName(state.displayName)
    }

    private fun UserPopupBottomsheetBinding.updateUserData(userState: UserPopupState.Success) {
        userAvatar.loadImage(userState.avatarUrl, placeholder = null, afterLoad = { userAvatarLoading.isVisible = false })
        userLoading.isVisible = false
        userGroup.isVisible = true
        userBlock.isEnabled = true
        userName.text = userState.userName.formatWithDisplayName(userState.displayName)
        userCreated.text = getString(R.string.user_popup_created, userState.created)
        userFollowage.text = userState.followingSince?.let {
            getString(R.string.user_popup_following_since, it)
        } ?: getString(R.string.user_popup_not_following)

        // Get subscription status
        var isSubscribed = false
        var subscriptionMonths = 0
        var subscriptionTier = 1
        for (badge in args.badges) {
            if (badge.badgeTag?.startsWith("subscriber/") == true) {
                isSubscribed = true
                subscriptionMonths = badge.badgeInfo?.toInt() ?: 0

                // Tier 2 and tier 3 have a '20' and '30' prefix.
                // For example: Tier 3, 2 years badge code is 3024.
                if ("subscriber\\/20\\d{2}".toRegex().matches(badge.badgeTag!!)) subscriptionTier = 2
                if ("subscriber\\/30\\d{2}".toRegex().matches(badge.badgeTag!!)) subscriptionTier = 3
            }
        }
        var subscriptionText = "";
        if (isSubscribed) {
            subscriptionText += "Tier ${subscriptionTier}. Subscribed for $subscriptionMonths month"
        }
        else if (subscriptionMonths > 0) {
            subscriptionText += "Previously subscribed for $subscriptionMonths month"
        }
        else {
            userSubscription.visibility = View.GONE
        }
        if (subscriptionMonths >= 2) subscriptionText += "s"
        userSubscription.text = subscriptionText

        userBlock.text = when {
            userState.isBlocked -> getString(R.string.user_popup_unblock)
            else                -> getString(R.string.user_popup_block)
        }
    }

    private fun UserPopupBottomsheetBinding.showNotLoggedInState(state: UserPopupState.NotLoggedIn) {
        userLoading.isVisible = false
        userGroup.isVisible = true
        userAvatarLoading.isVisible = false
        userAvatar.load(R.drawable.ic_person) {
            scale(Scale.FIT)
        }

        userName.text = state.userName.formatWithDisplayName(state.displayName)

        userMention.isEnabled = false
        userBlock.isEnabled = false
    }

    private fun setErrorResultAndDismiss(throwable: Throwable?) {
        findNavController()
            .getBackStackEntry(R.id.mainFragment)
            .savedStateHandle[MainFragment.USER_POPUP_RESULT_KEY] = UserPopupResult.Error(throwable)
        dialog?.dismiss()
    }

    private fun showTimeoutDialog() {
        var currentItem = 0
        val choices = resources.getStringArray(R.array.timeout_entries)
        val dialogContent = TimeoutDialogBinding.inflate(LayoutInflater.from(requireContext()), null, false).apply {
            timeoutSlider.setLabelFormatter { choices[it.toInt()] }
            timeoutSlider.addOnChangeListener { _, value, _ ->
                currentItem = value.toInt()
                timeoutValue.text = choices[value.toInt()]
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_user_timeout_title)
            .setView(dialogContent.root)
            .setPositiveButton(R.string.confirm_user_timeout_positive_button) { _, _ ->
                lifecycleScope.launch {
                    viewModel.timeoutUser(currentItem)
                    dialog?.dismiss()
                }
            }
            .setNegativeButton(R.string.dialog_cancel) { d, _ -> d.dismiss() }
            .show()
    }

    private fun showBanDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_user_ban_title)
            .setMessage(R.string.confirm_user_ban_message)
            .setPositiveButton(R.string.confirm_user_ban_positive_button) { _, _ ->
                lifecycleScope.launch {
                    viewModel.banUser()
                    dialog?.dismiss()
                }
            }
            .setNegativeButton(R.string.dialog_cancel) { d, _ -> d.dismiss() }
            .show()
    }

    private fun showDeleteDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_user_delete_title)
            .setMessage(R.string.confirm_user_delete_message)
            .setPositiveButton(R.string.confirm_user_delete_positive_button) { _, _ ->
                lifecycleScope.launch {
                    viewModel.deleteMessage()
                    dialog?.dismiss()
                }
            }
            .setNegativeButton(R.string.dialog_cancel) { d, _ -> d.dismiss() }
            .show()
    }
}
