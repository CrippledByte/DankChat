package com.flxrs.dankchat.channels

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.flxrs.dankchat.R
import com.flxrs.dankchat.databinding.ChannelsFragmentBinding
import com.flxrs.dankchat.main.MainFragment
import com.flxrs.dankchat.preferences.ChannelWithRename
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.utils.extensions.navigateSafe
import com.flxrs.dankchat.utils.extensions.swap
import com.flxrs.dankchat.utils.extensions.withData
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChannelsDialogFragment : BottomSheetDialogFragment() {

    @Inject
    lateinit var dankChatPreferences: DankChatPreferenceStore

    private var adapter: ChannelsAdapter? = null
    private val args: ChannelsDialogFragmentArgs by navArgs()
    private val navController: NavController by lazy { findNavController() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val channels = args.channels.toList()
        val withRenames = dankChatPreferences.getChannelsWithRenames(channels)
        adapter = ChannelsAdapter(dankChatPreferences, ::openRenameChannelDialog).also {
            it.submitList(withRenames)
            it.registerAdapterDataObserver(dataObserver)
        }
        val binding = ChannelsFragmentBinding.inflate(inflater, container, false).apply {
            channelsList.adapter = adapter
            val helper = ItemTouchHelper(itemTouchHelperCallback)
            helper.attachToRecyclerView(channelsList)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val navBackStackEntry = navController.getBackStackEntry(R.id.channelsDialogFragment)
        val handle = navBackStackEntry.savedStateHandle
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver
            handle.keys().forEach { key ->
                when (key) {
                    RENAME_TAB_REQUEST_KEY -> handle.withData(key, ::renameChannel)
                }
            }
        }
        navBackStackEntry.getLifecycle().addObserver(observer)
        viewLifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                navBackStackEntry.getLifecycle().removeObserver(observer)
            }
        })
    }

    override fun onDestroyView() {
        adapter?.unregisterAdapterDataObserver(dataObserver)
        adapter = null
        super.onDestroyView()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        adapter?.let {
            navController
                .getBackStackEntry(R.id.mainFragment)
                .savedStateHandle[MainFragment.CHANNELS_REQUEST_KEY] = it.currentList.toTypedArray()
        }

    }

    private fun openRenameChannelDialog(channelWithRename: ChannelWithRename) {
        val direction = ChannelsDialogFragmentDirections.actionChannelsFragmentToEditChannelDialogFragment(channelWithRename)
        navigateSafe(direction)
    }

    private fun renameChannel(channelWithRename: ChannelWithRename) {
        val adapter = adapter ?: return
        val updated = adapter.currentList.toMutableList()
        val position = updated.indexOfFirst { it.channel == channelWithRename.channel }
        updated[position] = channelWithRename
        adapter.submitList(updated)
    }

    private val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            val adapter = adapter ?: return false
            adapter.currentList.toMutableList().let {
                it.swap(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
                adapter.submitList(it)
            }
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
    }

    private val dataObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            val adapter = adapter ?: return
            if (adapter.currentList.isEmpty()) {
                dismiss()
            }
        }
    }

    companion object {
        private val TAG = ChannelsDialogFragment::class.java.simpleName

        const val RENAME_TAB_REQUEST_KEY = "rename_channel_key"
    }
}
