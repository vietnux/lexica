package com.serwylo.lexica

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.serwylo.lexica.databinding.ChooseGameModeBinding
import com.serwylo.lexica.databinding.GameModeItemBinding
import com.serwylo.lexica.db.Database
import com.serwylo.lexica.db.GameMode
import com.serwylo.lexica.db.GameModeRepository
import kotlin.collections.ArrayList
import kotlin.math.sqrt

class ChooseGameModeActivity : AppCompatActivity() {

    private lateinit var binding: ChooseGameModeBinding

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        ThemeManager.getInstance().applyTheme(this)

        binding = ChooseGameModeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.gameModeList.layoutManager = LinearLayoutManager(this)
        binding.gameModeList.setHasFixedSize(false)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        AsyncTask.execute {
            val repo = GameModeRepository(Database.get(applicationContext).gameModeDao(), PreferenceManager.getDefaultSharedPreferences(this))
            val current = repo.loadCurrentGameMode() ?: error("No game mode present, should have run database migrations prior to navigating to choose game mode activity.")
            runOnUiThread {
                binding.gameModeList.adapter = Adapter(current)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.choose_game_mode_menu, binding.toolbar.menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.new_game_mode) {
            addGameMode()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun addGameMode() {
        startActivity(Intent(this, NewGameModeActivity::class.java))
    }

    inner class Adapter(selectedItem: GameMode) : RecyclerView.Adapter<ViewHolder>() {

        private var gameModes: List<GameMode> = ArrayList()
        private var selectedItem: GameMode

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = GameModeItemBinding.inflate(this@ChooseGameModeActivity.layoutInflater, parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val mode = gameModes[position]
            holder.bind(mode, selectedItem.gameModeId == mode.gameModeId) { selectGameMode(mode) }
        }

        override fun getItemCount(): Int {
            return gameModes.size
        }

        init {
            Database.get(applicationContext)
                    .gameModeDao()
                    .getAllGameModes()
                    .observe(this@ChooseGameModeActivity, { gameModes ->
                        this@Adapter.gameModes = gameModes
                        notifyDataSetChanged()
                    })
            this.selectedItem = selectedItem
        }

        private fun selectGameMode(mode: GameMode) {
            val repo = GameModeRepository(Database.get(applicationContext).gameModeDao(), PreferenceManager.getDefaultSharedPreferences(applicationContext))
            repo.saveCurrentGameMode(mode)
            this.selectedItem = mode
            notifyDataSetChanged() // TODO: Just notify the previous and newly selected items.
        }
    }

    inner class ViewHolder(private val binding: GameModeItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(gameMode: GameMode, isSelected: Boolean, listener: View.OnClickListener?) {

            val context = this@ChooseGameModeActivity

            val boardWidth = sqrt(gameMode.boardSize.toDouble()).toInt()

            binding.root.setOnClickListener(listener)
            binding.root.isSelected = isSelected
            binding.label.text = gameMode.label(context)
            binding.description.text = gameMode.description(context)

            if (isSelected) {
                binding.statusTime.visibility = View.VISIBLE
                binding.statusBoardSize.visibility = View.VISIBLE
                binding.statusScoreType.visibility = View.VISIBLE
                binding.statusMinLength.visibility = View.VISIBLE

                binding.statusTime.setText(context.resources.getQuantityString(R.plurals.num_minutes, gameMode.timeLimitSeconds / 60, gameMode.timeLimitSeconds / 60))
                binding.statusBoardSize.setText("${boardWidth}x${boardWidth}")
                binding.statusScoreType.setText(if (gameMode.scoreType == "W") context.getString(R.string.word_length) else context.getString(R.string.letter_points))
                binding.statusMinLength.setText("≥ " + gameMode.minWordLength) // TODO: RTL

                if (gameMode.hintModeColor() || gameMode.hintModeCount()) {
                    binding.statusHintMode.visibility = View.VISIBLE
                    binding.statusHintMode.setText(context.getString(R.string.pref_hintMode))
                } else {
                    binding.statusHintMode.visibility = View.GONE
                }
            } else {
                binding.statusTime.visibility = View.GONE
                binding.statusBoardSize.visibility = View.GONE
                binding.statusScoreType.visibility = View.GONE
                binding.statusHintMode.visibility = View.GONE
                binding.statusMinLength.visibility = View.GONE
            }


        }
    }
}