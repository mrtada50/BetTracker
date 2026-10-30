package com.bettracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

private val Green = Color(0xFF4CAF50)
private val Red = Color(0xFFF44336)

fun fmt(d: Double): String = String.format(Locale.US, "%.2f", d)
private fun profitColor(d: Double) = if (d >= 0) Green else Red

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) { App() }
            }
        }
    }
}

@Composable
fun App() {
    val vm: BetVm = viewModel()
    val bets by vm.bets.collectAsState()
    val capital by vm.capital.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("🏠" to "الرئيسية", "➕" to "إضافة", "📋" to "السجل", "📊" to "التحليلات")

    Scaffold(bottomBar = {
        NavigationBar {
            tabs.forEachIndexed { i, (icon, label) ->
                NavigationBarItem(
                    selected = tab == i,
                    onClick = { tab = i },
                    icon = { Text(icon) },
                    label = { Text(label) }
                )
            }
        }
    }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> HomeScreen(bets, capital) { vm.setCapital(it) }
                1 -> AddScreen { vm.add(it); tab = 2 }
                2 -> HistoryScreen(bets, { b, s -> vm.setStatus(b, s) }, { vm.remove(it) })
                else -> StatsScreen(bets)
            }
        }
    }
}

@Composable
fun HomeScreen(bets: List<Bet>, capital: Double, onCapital: (Double) -> Unit) {
    val s = summarize(bets)
    val pending = bets.count { it.status == "PENDING" }
    var show by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("الرئيسية", style = MaterialTheme.typography.headlineMedium)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("الرصيد الحالي")
                Text(fmt(capital + s.profit), style = MaterialTheme.typography.headlineLarge)
                Text("رأس المال: ${fmt(capital)}")
                TextButton(onClick = { text = fmt(capital); show = true }) { Text("تعديل رأس المال") }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("صافي الربح / الخسارة")
                Text(
                    fmt(s.profit),
                    style = MaterialTheme.typography.headlineMedium,
                    color = profitColor(s.profit)
                )
                Text("ROI: ${fmt(s.roi)}%")
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("ناجحة: ${s.won}")
                Text("فاشلة: ${s.lost}")
                Text("معلّقة: $pending")
                Text("نسبة النجاح: ${fmt(s.winRate)}%")
            }
        }
    }

    if (show) {
        AlertDialog(
            onDismissRequest = { show = false },
            title = { Text("رأس المال") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    text.toDoubleOrNull()?.let(onCapital)
                    show = false
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text("إلغاء") } }
        )
    }
}

@Composable
fun <T> Choices(items: List<T>, selected: T, label: (T) -> String, onPick: (T) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            val sel = item == selected
            Box(
                Modifier.clip(RoundedCornerShape(20.dp))
                    .background(
                        if (sel) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onPick(item) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    label(item),
                    color = if (sel) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AddScreen(onSave: (Bet) -> Unit) {
    var match by rememberSaveable { mutableStateOf("") }
    var selection by rememberSaveable { mutableStateOf("") }
    var odds by rememberSaveable { mutableStateOf("") }
    var stake by rememberSaveable { mutableStateOf("") }
    var market by rememberSaveable { mutableStateOf(Market.OVER_UNDER) }
    var mode by rememberSaveable { mutableStateOf(Mode.PRE) }
    var error by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("إضافة رهان", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = match, onValueChange = { match = it },
            label = { Text("المباراة (ليفربول × تشيلسي)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Text("نوع السوق")
        Choices(Market.values().toList(), market, { it.label }) { market = it }
        OutlinedTextField(
            value = selection, onValueChange = { selection = it },
            label = { Text("الاختيار (Corners O9.5)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = odds, onValueChange = { odds = it },
                label = { Text("Odds") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = stake, onValueChange = { stake = it },
                label = { Text("المبلغ") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }
        Text("وقت الرهان")
        Choices(Mode.values().toList(), mode, { it.label }) { mode = it }
        if (error.isNotEmpty()) Text(error, color = Red)
        Button(
            onClick = {
                val o = odds.toDoubleOrNull()
                val s = stake.toDoubleOrNull()
                if (match.isBlank() || o == null || o <= 1.0 || s == null || s <= 0.0) {
                    error = "تأكد من المباراة والـ Odds (أكبر من 1) والمبلغ"
                } else {
                    error = ""
                    onSave(
                        Bet(
                            matchName = match.trim(), selection = selection.trim(),
                            market = market.name, odds = o, stake = s, mode = mode.name
                        )
                    )
                    match = ""; selection = ""; odds = ""; stake = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("حفظ الرهان") }
    }
}

@Composable
fun HistoryScreen(bets: List<Bet>, onStatus: (Bet, Status) -> Unit, onDelete: (Bet) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            "سجل الرهانات", style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        if (bets.isEmpty()) Text("لا توجد رهانات بعد")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(bets, key = { it.id }) { b ->
                val mk = Market.valueOf(b.market).label
                val md = Mode.valueOf(b.mode).label
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(b.matchName, style = MaterialTheme.typography.titleMedium)
                        Text("$mk • ${b.selection} • $md")
                        Text("Odds ${fmt(b.odds)} × ${fmt(b.stake)}")
                        Text(
                            "${Status.valueOf(b.status).label}  ${if (b.settled) fmt(b.profit) else ""}",
                            color = if (b.settled) profitColor(b.profit) else MaterialTheme.colorScheme.onSurface
                        )
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            Status.values().forEach { s ->
                                TextButton(onClick = { onStatus(b, s) }) {
                                    Text(s.label, color = if (b.status == s.name) Green else MaterialTheme.colorScheme.primary)
                                }
                            }
                            TextButton(onClick = { onDelete(b) }) { Text("حذف", color = Red) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, s: Summary) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text("نجاح: ${fmt(s.winRate)}%  (${s.won} ناجح / ${s.lost} فاشل)")
            Text("الصافي: ${fmt(s.profit)}", color = profitColor(s.profit))
            Text("ROI: ${fmt(s.roi)}%")
        }
    }
}

@Composable
fun StatsScreen(bets: List<Bet>) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("التحليلات", style = MaterialTheme.typography.headlineMedium)
        StatCard("الإجمالي", summarize(bets))
        Text("حسب السوق", style = MaterialTheme.typography.titleLarge)
        Market.values().forEach { m ->
            val l = bets.filter { it.market == m.name }
            if (l.isNotEmpty()) StatCard(m.label, summarize(l))
        }
        Text("Pre-match مقابل Live", style = MaterialTheme.typography.titleLarge)
        Mode.values().forEach { m ->
            val l = bets.filter { it.mode == m.name }
            if (l.isNotEmpty()) StatCard(m.label, summarize(l))
        }
        if (bets.none { it.settled }) Text("أضف رهانات وحدّد نتائجها لتظهر الإحصائيات")
    }
}
