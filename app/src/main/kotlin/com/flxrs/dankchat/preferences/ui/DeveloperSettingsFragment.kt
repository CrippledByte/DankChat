package com.flxrs.dankchat.preferences.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import com.flxrs.dankchat.R
import com.flxrs.dankchat.databinding.ImportSettingsBottomsheetBinding
import com.flxrs.dankchat.databinding.RmHostBottomsheetBinding
import com.flxrs.dankchat.databinding.SettingsFragmentBinding
import com.flxrs.dankchat.main.MainActivity
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.preferences.ui.highlights.HighlightsTabAdapter
import com.flxrs.dankchat.preferences.ui.highlights.HighlightsViewModel
import com.flxrs.dankchat.preferences.ui.highlights.MessageHighlightItem
import com.flxrs.dankchat.preferences.ui.highlights.UserHighlightItem
import com.flxrs.dankchat.utils.ChatterinoSettingsModel
import com.flxrs.dankchat.utils.extensions.collectFlow
import com.flxrs.dankchat.utils.extensions.showRestartRequired
import com.flxrs.dankchat.utils.extensions.withTrailingSlash
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class DeveloperSettingsFragment : MaterialPreferenceFragmentCompat() {

    private val highlightsViewModel: HighlightsViewModel by viewModels()
    private var bottomSheetDialog: BottomSheetDialog? = null

    @Inject
    lateinit var dankChatPreferenceStore: DankChatPreferenceStore

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = SettingsFragmentBinding.bind(view)
        (requireActivity() as MainActivity).apply {
            setSupportActionBar(binding.settingsToolbar)
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                title = getString(R.string.preference_developer_header)
            }
        }

        findPreference<Preference>(getString(R.string.preference_rm_host_key))?.apply {
            setOnPreferenceClickListener { showRmHostPreference(view) }
        }

        findPreference<Preference>(getString(R.string.preference_import_settings_key))?.apply {
            setOnPreferenceClickListener { showImportSettingsPreference(view) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bottomSheetDialog?.dismiss()
        bottomSheetDialog = null
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.developer_settings, rootKey)
    }

    private fun showRmHostPreference(root: View): Boolean {
        val context = root.context
        val windowHeight = resources.displayMetrics.heightPixels
        val peekHeight = (windowHeight * 0.6).roundToInt()
        val currentHost = dankChatPreferenceStore.customRmHost
        val binding = RmHostBottomsheetBinding.inflate(LayoutInflater.from(context), root as? ViewGroup, false).apply {
            rmHostInput.setText(currentHost)
            hostReset.setOnClickListener {
                rmHostInput.setText(dankChatPreferenceStore.resetRmHost())
            }
            rmHostSheet.updateLayoutParams {
                height = windowHeight
            }
        }

        bottomSheetDialog = BottomSheetDialog(context).apply {
            setContentView(binding.root)
            setOnDismissListener {
                val newHost = binding.rmHostInput.text
                    ?.toString()
                    ?.withTrailingSlash ?: return@setOnDismissListener

                if (newHost != currentHost) {
                    dankChatPreferenceStore.customRmHost = newHost
                    view?.showRestartRequired()
                }
            }
            behavior.isFitToContents = false
            behavior.peekHeight = peekHeight
            show()
        }

        return true
    }

    private fun showImportSettingsPreference(root: View): Boolean {
        val context = root.context
        val windowHeight = resources.displayMetrics.heightPixels
        val peekHeight = (windowHeight * 0.6).roundToInt()
        var selectAllCheckboxes = false

        val binding = ImportSettingsBottomsheetBinding.inflate(LayoutInflater.from(context), root as? ViewGroup, false).apply {
            lateinit var data: ChatterinoSettingsModel

            importSettingsCheckboxContainer.visibility = View.GONE

            importSettingsPaste.setOnClickListener {
                selectAllCheckboxes = false
                var text = ""

                // Get text from clipboard
                (requireActivity().getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).apply {
                    if (primaryClip == null) return@apply

                    if (primaryClip!!.itemCount > 0) {
                        text = primaryClip!!.getItemAt(0).text.toString()
                    }
                }

                // Parse JSON
                val moshi: Moshi = Moshi.Builder().build()
                val adapter: JsonAdapter<ChatterinoSettingsModel> = moshi.adapter(ChatterinoSettingsModel::class.java).serializeNulls()

                try {
                    data = adapter.fromJson(text)!!
                    importSettingsPasteInfo.text = ""
                    importSettingsCheckboxContainer.visibility = View.VISIBLE
                    importSettingsPaste.isEnabled = false

                    // Disable checkboxes for unavailable items
                    if (data.highlighting.users.isEmpty()) importSettingsCheckboxUserHighlights.isEnabled = false
                    if (data.highlighting.highlights.isEmpty()) importSettingsCheckboxMessageHighlights.isEnabled = false
                } catch (e: IOException) {
                    importSettingsPasteInfo.text = "Could not read settings.\nCheck if you have copied the JSON text."
                } catch (e: JsonDataException) {
                    importSettingsPasteInfo.text = "Could not read settings.\nIncompatible format."
                }
            }

            importSettingsSelectAll.setOnClickListener {
                fun setCheckboxes(checked: Boolean) {
                    importSettingsCheckboxAppearance.isChecked = checked
                    importSettingsCheckboxChat.isChecked = checked
                    if (importSettingsCheckboxMessageHighlights.isEnabled) importSettingsCheckboxMessageHighlights.isChecked = checked
                    if (importSettingsCheckboxUserHighlights.isEnabled) importSettingsCheckboxUserHighlights.isChecked = checked
                }

                selectAllCheckboxes = !selectAllCheckboxes

                setCheckboxes(selectAllCheckboxes)

                if (selectAllCheckboxes)
                    importSettingsSelectAll.text = "Select none"
                else
                    importSettingsSelectAll.text = "Select all"
            }

            importSettingsImport.setOnClickListener {
                fun showRestartSnackbar() {
                    // Show snackbar with restart button (similar to showRestartRequired() but on top of bottom sheet)
                    Snackbar.make(it, context.getString(R.string.restart_required), Snackbar.LENGTH_LONG)
                        .setAction(R.string.restart, View.OnClickListener {
                            val restartIntent = Intent(context, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            }
                            context.startActivity(restartIntent)
                            Runtime.getRuntime().exit(0)
                        })
                        .show()
                }

                val sharedPreferences: SharedPreferences = context.getSharedPreferences(context.getString(R.string.shared_preference_key), Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()

                if (importSettingsCheckboxAppearance.isChecked) {
                    //// Separate messages with lines
                    editor.putBoolean(context.getString(R.string.preference_line_separator_key), data.appearance.messages.separateMessages)

                    //// Alternate messages background
                    editor.putBoolean(context.getString(R.string.checkered_messages_key), data.appearance.messages.alternateMessageBackground)
                }

                val highlightsAdapter = HighlightsTabAdapter(
                    onAddItem = highlightsViewModel::addHighlight,
                    onDeleteItem = highlightsViewModel::removeHighlight,
                    preferences = dankChatPreferenceStore,
                )

                collectFlow(highlightsViewModel.highlightTabs, highlightsAdapter::submitList)

                if (importSettingsCheckboxMessageHighlights.isChecked) {
                    //// Message highlights
                    for (entry in data.highlighting.highlights) {
                        val item = MessageHighlightItem(
                            id = 0,
                            enabled = entry.alert,
                            type = MessageHighlightItem.Type.Custom,
                            pattern = entry.pattern,
                            isRegex = entry.regex,
                            isCaseSensitive = entry.case,
                            createNotification = true,
                        )
                        highlightsViewModel.addHighlightItem(item, 0)
                    }
                }

                if (importSettingsCheckboxUserHighlights.isChecked) {
                    //// User highlights
                    for (entry in data.highlighting.users) {
                        val item = UserHighlightItem(
                            id = 0,
                            enabled = entry.alert,
                            username = entry.pattern,
                            createNotification = true,
                        )
                        highlightsViewModel.addHighlightItem(item, 1)
                    }
                }

                if (importSettingsCheckboxChat.isChecked) {
                    if (data.emotes != null) {
                        //// Show/hide unlisted emotes
                        if (data.emotes!!.showUnlistedEmotes != null) {
                            editor.putBoolean(context.getString(R.string.preference_unlisted_emotes_key), data.emotes!!.showUnlistedEmotes!!)
                            if (data.emotes?.showUnlistedEmotes != sharedPreferences.getBoolean(context.getString(R.string.preference_unlisted_emotes_key), false)) {
                                showRestartSnackbar()
                            }
                        }

                        //// Animate gifs
                        if (data.emotes!!.enableGifAnimations != null) {
                            editor.putBoolean(context.getString(R.string.preference_animate_gifs_key), data.emotes!!.enableGifAnimations!!)
                        }
                    }

                    //// Message scrollback limit
                    // limit value to keep within DankChat history limit range,
                    // divide to convert to values 1-20 which DankChat uses to store history limit
                    val length = Integer.max(50, Integer.min(1000, data.misc.twitch.messageHistoryLimit)) / 50
                    editor.putInt(context.getString(R.string.preference_scrollback_length_key), length)

                    /// Visible badges
                    val badges: MutableSet<String> = mutableSetOf()
                    if (data.appearance.badges.vanity) badges.add("vanity")
                    if (data.appearance.badges.ChannelAuthority) badges.add("channel")
                    if (data.appearance.badges.predictions) badges.add("predictions")
                    if (data.appearance.badges.GlobalAuthority) badges.add("authority")
                    if (data.appearance.badges.subscription != null) // does not exist on non-7tv chatterino
                        if (data.appearance.badges.subscription!!) badges.add("subscriber")
                    editor.putStringSet(context.getString(R.string.preference_visible_badges_key), badges)

                    //// Show/hide timed out messages
                    editor.putBoolean(context.getString(R.string.preference_show_timed_out_messages_key), !data.appearance.messages.hideModerated)

                    //// Show/hide timestamps
                    editor.putBoolean(context.getString(R.string.preference_timestamp_key), data.appearance.messages.showTimestamps)

                    //// Timestamp format
                    val timestampFormats = resources.getStringArray(R.array.timestamp_formats)
                    val timestampFormatImport: String = data.appearance.messages.timestampFormat

                    // Get DankChat timestamp array index
                    // DankChat uses uppercase hour letters for 24-hour format.
                    // Convert DankChat's HH:mm:ss to lowercase so Chatterino's hh:mm:ss can be matched.
                    val timestampFormatsLower = timestampFormats.map { format -> format.lowercase() }
                    val timestampFormatIndex = timestampFormatsLower.indexOf(timestampFormatImport)

                    // Save if format exists in DankChat
                    if (timestampFormatIndex >= 0) {
                        editor.putString(context.getString(R.string.preference_timestamp_format_key), timestampFormats[timestampFormatIndex])
                    }

                    //// Load history on startup
                    // Key/value pair not present in settings.json if history is set to load in Chatterino
                    if (data.misc.twitch.loadMessageHistoryOnConnect != null) {
                        editor.putBoolean(context.getString(R.string.preference_load_message_history_key), data.misc.twitch.loadMessageHistoryOnConnect!!)
                    } else {
                        editor.putBoolean(context.getString(R.string.preference_load_message_history_key), true)
                    }
                }

                // Save changes
                editor.apply()

                // Disable checkboxes
                importSettingsCheckboxAppearance.isEnabled = false
                importSettingsCheckboxChat.isEnabled = false
                importSettingsCheckboxUserHighlights.isEnabled = false
                importSettingsCheckboxMessageHighlights.isEnabled = false

                // Disable buttons, show text
                importSettingsSelectAll.isEnabled = false
                importSettingsImport.isEnabled = false
                importSettingsImportInfo.text = "Completed!"
            }

            importSettingsSheet.updateLayoutParams {
                height = windowHeight
            }
        }

        BottomSheetDialog(context).apply {
            setContentView(binding.root)
            behavior.isFitToContents = false
            behavior.peekHeight = peekHeight
            show()
        }

        return true
    }
}