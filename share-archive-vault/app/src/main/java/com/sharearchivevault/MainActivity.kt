package com.sharearchivevault

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.tabs.TabLayoutMediator
import com.sharearchivevault.databinding.ActivityMainBinding
import com.sharearchivevault.model.MediaItem
import com.sharearchivevault.ui.ExtractionState
import com.sharearchivevault.ui.MainViewModel
import com.sharearchivevault.ui.MediaFragment
import com.sharearchivevault.ui.VaultPagerAdapter
import com.sharearchivevault.util.CacheManager
import com.sharearchivevault.util.CacheWipeWorker
import kotlinx.coroutines.launch

/**
 * Entry-point activity.
 *
 * Handles two launch modes:
 *  1. Direct launch → shows a helpful "Share a WhatsApp ZIP" instruction screen.
 *  2. Share/VIEW intent with a ZIP → begins extraction immediately.
 *
 * Privacy guarantee: onDestroy() triggers a background cache wipe via WorkManager
 * so files are removed even if the process is killed after the activity is gone.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTabs()
        setupShareButton()
        observeState()

        // Process the incoming intent (either on first create or re-delivered)
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIncomingIntent(it) }
    }

    // ── Intent Handling ─────────────────────────────────────────────────────

    private fun handleIncomingIntent(intent: Intent) {
        val zipUri: Uri? = when (intent.action) {
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
            }
            Intent.ACTION_VIEW -> intent.data
            else -> null
        }

        if (zipUri != null) {
            viewModel.processZip(this, zipUri)
        } else {
            // Launched directly without a ZIP — show idle welcome state
        }
    }

    // ── Tab Setup ────────────────────────────────────────────────────────────

    private fun setupTabs() {
        val pagerAdapter = VaultPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_photos)
                1 -> getString(R.string.tab_videos)
                else -> ""
            }
        }.attach()
    }

    // ── Share Button ─────────────────────────────────────────────────────────

    private fun setupShareButton() {
        binding.btnShare.setOnClickListener {
            val selected = viewModel.getSelectedItems()
            if (selected.isEmpty()) {
                Toast.makeText(this, getString(R.string.select_files_prompt), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            shareFiles(selected)
        }
    }

    private fun shareFiles(items: List<MediaItem>) {
        val uris = items.mapNotNull { item ->
            try {
                FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    item.file
                )
            } catch (e: Exception) {
                null
            }
        }

        if (uris.isEmpty()) {
            Toast.makeText(this, getString(R.string.share_error), Toast.LENGTH_SHORT).show()
            return
        }

        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)))
    }

    // ── State Observer ────────────────────────────────────────────────────────

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is ExtractionState.Idle -> {
                            binding.progressBar.visibility = View.GONE
                            binding.idleHint.visibility = View.VISIBLE
                            binding.contentLayout.visibility = View.GONE
                            binding.btnShare.visibility = View.GONE
                        }
                        is ExtractionState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.idleHint.visibility = View.GONE
                            binding.contentLayout.visibility = View.GONE
                            binding.btnShare.visibility = View.GONE
                        }
                        is ExtractionState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.idleHint.visibility = View.GONE
                            binding.contentLayout.visibility = View.VISIBLE
                            binding.btnShare.visibility = View.VISIBLE
                            val total = state.photos.size + state.videos.size
                            binding.tvStatus.text = getString(R.string.files_found, total)
                        }
                        is ExtractionState.Empty -> {
                            binding.progressBar.visibility = View.GONE
                            binding.idleHint.visibility = View.GONE
                            binding.contentLayout.visibility = View.VISIBLE
                            binding.btnShare.visibility = View.GONE
                            binding.tvStatus.text = getString(R.string.no_media_found)
                        }
                        is ExtractionState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.idleHint.visibility = View.VISIBLE
                            binding.contentLayout.visibility = View.GONE
                            binding.btnShare.visibility = View.GONE
                            Toast.makeText(this@MainActivity, state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    // ── Privacy / Cache Lifecycle ─────────────────────────────────────────────

    override fun onDestroy() {
        super.onDestroy()
        // Immediate synchronous clear (best-effort on main thread before process dies)
        CacheManager.clearAll(this)
        // Durable background wipe via WorkManager as a safety net
        val wipeRequest = OneTimeWorkRequestBuilder<CacheWipeWorker>().build()
        WorkManager.getInstance(this).enqueue(wipeRequest)
    }
}
