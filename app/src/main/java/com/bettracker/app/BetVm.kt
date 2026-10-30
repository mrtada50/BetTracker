package com.bettracker.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BetVm(app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.get(app).dao()
    private val prefs = app.getSharedPreferences("prefs", 0)

    val bets = dao.all().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _capital = MutableStateFlow(prefs.getString("capital", "0")?.toDoubleOrNull() ?: 0.0)
    val capital = _capital.asStateFlow()

    fun setCapital(v: Double) {
        prefs.edit().putString("capital", v.toString()).apply()
        _capital.value = v
    }

    fun add(b: Bet) = viewModelScope.launch { dao.insert(b) }
    fun setStatus(b: Bet, s: Status) = viewModelScope.launch { dao.update(b.copy(status = s.name)) }
    fun remove(b: Bet) = viewModelScope.launch { dao.delete(b) }
}
