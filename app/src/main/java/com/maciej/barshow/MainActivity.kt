package com.maciej.barshow

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.tv.material3.*
import com.maciej.barshow.ui.theme.BarShowTheme
import java.sql.DriverManager

private const val TAG = "BarShowDB"

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BarShowTheme {
                Surface(modifier = Modifier.fillMaxSize(), shape = RectangleShape) {
                    MyApp()
                }
            }
        }
    }
}

// --- PERSISTENCE HELPERS ---

fun saveMatchProgress(context: Context, left: Int, right: Int, history: List<String>, setScores: List<String>, startingPlayer: Int?, p1: String, p2: String, sessionId: Long) {
    val prefs = context.getSharedPreferences("MatchProgress", Context.MODE_PRIVATE)
    prefs.edit().apply {
        putInt("left", left)
        putInt("right", right)
        putString("history", history.joinToString(","))
        putString("setScores", setScores.joinToString(","))
        putInt("startingPlayer", startingPlayer ?: 0)
        putString("p1", p1)
        putString("p2", p2)
        putLong("sessionId", sessionId)
        putBoolean("exists", true)
        apply()
    }
}

fun clearMatchProgress(context: Context) {
    context.getSharedPreferences("MatchProgress", Context.MODE_PRIVATE).edit().clear().apply()
}

fun saveProfiles(context: Context, profiles: List<String>) {
    val prefs = context.getSharedPreferences("BarShowPrefs", Context.MODE_PRIVATE)
    prefs.edit().putStringSet("profiles", profiles.toSet()).apply()
}

fun loadProfiles(context: Context): List<String> {
    val prefs = context.getSharedPreferences("BarShowPrefs", Context.MODE_PRIVATE)
    return prefs.getStringSet("profiles", emptySet())?.toList() ?: emptyList()
}

@Composable
fun MyApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    
    val genPrefs = context.getSharedPreferences("GeneralSettings", Context.MODE_PRIVATE)
    var changeSidesEnabled by remember { mutableStateOf(genPrefs.getBoolean("changeSides", true)) }
    var changeSidesAnimationEnabled by remember { mutableStateOf(genPrefs.getBoolean("changeSidesAnim", true)) }
    var player1Name by remember { mutableStateOf(genPrefs.getString("p1Name", "Gracz 1") ?: "Gracz 1") }
    var player2Name by remember { mutableStateOf(genPrefs.getString("p2Name", "Gracz 2") ?: "Gracz 2") }
    
    val profiles = remember { mutableStateListOf<String>().apply { addAll(loadProfiles(context)) } }

    val dbPrefs = context.getSharedPreferences("DatabasePrefs", Context.MODE_PRIVATE)
    var dbHost by remember { mutableStateOf(dbPrefs.getString("host", "") ?: "") }
    var dbPort by remember { mutableStateOf(dbPrefs.getString("port", "3306") ?: "3306") }
    var dbName by remember { mutableStateOf(dbPrefs.getString("name", "") ?: "") }
    var dbUser by remember { mutableStateOf(dbPrefs.getString("user", "") ?: "") }
    var dbPass by remember { mutableStateOf(dbPrefs.getString("pass", "") ?: "") }

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                onNavigateToDetails = { navController.navigate("details/false") },
                onNavigateToResume = { navController.navigate("details/true") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToProfiles = { navController.navigate("profiles") }
            )
        }
        composable(
            "details/{resume}",
            arguments = listOf(navArgument("resume") { type = NavType.BoolType })
        ) { backStackEntry ->
            val resume = backStackEntry.arguments?.getBoolean("resume") ?: false
            DetailsScreen(
                navController = navController,
                changeSidesEnabled = changeSidesEnabled,
                changeSidesAnimationEnabled = changeSidesAnimationEnabled,
                player1Name = player1Name,
                player2Name = player2Name,
                resume = resume,
                dbConfig = if (dbHost.isNotBlank()) mapOf("host" to dbHost, "port" to dbPort, "name" to dbName, "user" to dbUser, "pass" to dbPass) else null
            )
        }
        composable("settings") {
            SettingsScreen(
                navController = navController,
                changeSidesEnabled = changeSidesEnabled,
                onToggleChangeSides = { changeSidesEnabled = it },
                changeSidesAnimationEnabled = changeSidesAnimationEnabled,
                onToggleChangeSidesAnimation = { changeSidesAnimationEnabled = it },
                player1Name = player1Name,
                onPlayer1NameChange = { player1Name = it },
                player2Name = player2Name,
                onPlayer2NameChange = { player2Name = it },
                dbHost = dbHost, onDbHostChange = { dbHost = it },
                dbPort = dbPort, onDbPortChange = { dbPort = it },
                dbName = dbName, onDbNameChange = { dbName = it },
                dbUser = dbUser, onDbUserChange = { dbUser = it },
                dbPass = dbPass, onDbPassChange = { dbPass = it }
            )
        }
        composable("profiles") {
            ProfilesScreen(
                navController = navController,
                profiles = profiles,
                onPlayersSelected = { p1, p2 ->
                    player1Name = p1
                    player2Name = p2
                    genPrefs.edit().putString("p1Name", p1).putString("p2Name", p2).apply()
                    navController.popBackStack("main", inclusive = false)
                }
            )
        }
        composable("add_profile") {
            AddProfileScreen(
                navController = navController,
                profiles = profiles,
                onProfileAdded = { saveProfiles(context, profiles) }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MainScreen(onNavigateToDetails: () -> Unit, onNavigateToResume: () -> Unit, onNavigateToSettings: () -> Unit, onNavigateToProfiles: () -> Unit) {
    val context = LocalContext.current
    val hasSavedMatch = remember { context.getSharedPreferences("MatchProgress", Context.MODE_PRIVATE).getBoolean("exists", false) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Wybierz grę", style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(bottom = 48.dp))
            
            if (hasSavedMatch) {
                Button(onClick = onNavigateToResume, modifier = Modifier.size(width = 240.dp, height = 56.dp)) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Kontynuuj mecz", style = MaterialTheme.typography.labelLarge, color = Color.Yellow) }
                }
                Spacer(modifier = Modifier.size(16.dp))
            }

            Button(onClick = onNavigateToDetails, modifier = Modifier.size(width = 240.dp, height = 56.dp)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nowy mecz", style = MaterialTheme.typography.labelLarge) }
            }
            Spacer(modifier = Modifier.size(16.dp))
            Button(onClick = onNavigateToProfiles, modifier = Modifier.size(width = 240.dp, height = 56.dp)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Profile", style = MaterialTheme.typography.labelLarge) }
            }
            Spacer(modifier = Modifier.size(16.dp))
            Button(onClick = onNavigateToSettings, modifier = Modifier.size(width = 240.dp, height = 56.dp)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Ustawienia", style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfilesScreen(navController: NavController, profiles: MutableList<String>, onPlayersSelected: (String, String) -> Unit) {
    val selectedPlayers = remember { mutableStateListOf<String>() }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp).fillMaxWidth()) {
            Text("Profile Graczy", style = MaterialTheme.typography.displaySmall)
            if (selectedPlayers.isNotEmpty()) {
                Text("Wybrano: ${selectedPlayers.joinToString(" vs ")}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(modifier = Modifier.size(40.dp))
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, maxItemsInEachRow = 5) {
                profiles.forEach { profile ->
                    val isSelected = selectedPlayers.contains(profile)
                    
                    Surface(
                        onClick = {
                            if (isSelected) {
                                selectedPlayers.remove(profile)
                            } else if (selectedPlayers.size < 2) {
                                selectedPlayers.add(profile)
                                if (selectedPlayers.size == 2) {
                                    onPlayersSelected(selectedPlayers[0], selectedPlayers[1])
                                }
                            }
                        },
                        shape = ClickableSurfaceDefaults.shape(CircleShape),
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            focusedContainerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = ClickableSurfaceDefaults.border(
                            focusedBorder = Border(BorderStroke(3.dp, Color.White)),
                            border = if (isSelected) Border(BorderStroke(3.dp, MaterialTheme.colorScheme.primary)) else Border.None
                        ),
                        modifier = Modifier.size(110.dp).padding(8.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = profile, 
                                style = MaterialTheme.typography.bodyMedium, 
                                textAlign = TextAlign.Center, 
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, 
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
                
                Surface(
                    onClick = { navController.navigate("add_profile") },
                    shape = ClickableSurfaceDefaults.shape(CircleShape),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        focusedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    border = ClickableSurfaceDefaults.border(
                        focusedBorder = Border(BorderStroke(3.dp, Color.White)),
                        border = Border(BorderStroke(3.dp, MaterialTheme.colorScheme.primary))
                    ),
                    modifier = Modifier.size(110.dp).padding(8.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("+", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(modifier = Modifier.size(48.dp))
            Button(onClick = { navController.popBackStack() }) { Text("Powrót") }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AddProfileScreen(navController: NavController, profiles: MutableList<String>, onProfileAdded: () -> Unit) {
    var newProfileName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                .padding(32.dp)
                .fillMaxWidth(0.45f), 
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Nowy Profil", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.size(24.dp))
            ModernTextField(
                value = newProfileName, 
                onValueChange = { newProfileName = it; errorMessage = null }, 
                label = "Nazwa gracza", 
                modifier = Modifier.focusRequester(focusRequester)
            )
            if (errorMessage != null) Text(errorMessage!!, color = Color.Red, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            Spacer(modifier = Modifier.size(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { navController.popBackStack() }) { Text("Anuluj") }
                Button(onClick = {
                    val trimmed = newProfileName.trim()
                    if (trimmed.isEmpty()) errorMessage = "Nazwa nie może być pusta"
                    else if (profiles.any { it.equals(trimmed, ignoreCase = true) }) errorMessage = "Taki profil już istnieje"
                    else { profiles.add(trimmed); onProfileAdded(); navController.popBackStack() }
                }) { Text("Dodaj") }
            }
        }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    changeSidesEnabled: Boolean, onToggleChangeSides: (Boolean) -> Unit,
    changeSidesAnimationEnabled: Boolean, onToggleChangeSidesAnimation: (Boolean) -> Unit,
    player1Name: String, onPlayer1NameChange: (String) -> Unit,
    player2Name: String, onPlayer2NameChange: (String) -> Unit,
    dbHost: String, onDbHostChange: (String) -> Unit,
    dbPort: String, onDbPortChange: (String) -> Unit,
    dbName: String, onDbNameChange: (String) -> Unit,
    dbUser: String, onDbUserChange: (String) -> Unit,
    dbPass: String, onDbPassChange: (String) -> Unit
) {
    val context = LocalContext.current
    var showDebug by remember { mutableStateOf(false) }
    var debugLog by remember { mutableStateOf("Oczekiwanie na test...") }

    BackHandler {
        val genPrefs = context.getSharedPreferences("GeneralSettings", Context.MODE_PRIVATE)
        genPrefs.edit().putBoolean("changeSides", changeSidesEnabled).putBoolean("changeSidesAnim", changeSidesAnimationEnabled).putString("p1Name", player1Name).putString("p2Name", player2Name).apply()
        val dbPrefs = context.getSharedPreferences("DatabasePrefs", Context.MODE_PRIVATE)
        dbPrefs.edit().putString("host", dbHost).putString("port", dbPort).putString("name", dbName).putString("user", dbUser).putString("pass", dbPass).apply()
        navController.popBackStack()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize().padding(32.dp)) {
            Text("Ustawienia", style = MaterialTheme.typography.displaySmall)
            Spacer(modifier = Modifier.size(24.dp))
            Row(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Ogólne", style = MaterialTheme.typography.headlineSmall)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(16.dp)) {
                        Text("Zmiana stron", style = MaterialTheme.typography.titleMedium)
                        androidx.compose.material3.Switch(
                            checked = changeSidesEnabled, 
                            onCheckedChange = onToggleChangeSides, 
                            thumbContent = null,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(16.dp)) {
                        Text("Animacja zmiany stron", style = MaterialTheme.typography.titleMedium)
                        androidx.compose.material3.Switch(
                            checked = changeSidesAnimationEnabled, 
                            onCheckedChange = onToggleChangeSidesAnimation, 
                            thumbContent = null,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                    }
                    ModernTextField(value = player1Name, onValueChange = onPlayer1NameChange, label = "Domyślna Nazwa P1")
                    ModernTextField(value = player2Name, onValueChange = onPlayer2NameChange, label = "Domyślna Nazwa P2")
                }
                Column(modifier = Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Baza Danych", style = MaterialTheme.typography.headlineSmall)
                    ModernTextField(value = dbHost, onValueChange = onDbHostChange, label = "Host")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) { ModernTextField(value = dbPort, onValueChange = onDbPortChange, label = "Port") }
                        Box(modifier = Modifier.weight(2f)) { ModernTextField(value = dbName, onValueChange = onDbNameChange, label = "Baza") }
                    }
                    ModernTextField(value = dbUser, onValueChange = onDbUserChange, label = "Użytkownik")
                    ModernTextField(value = dbPass, onValueChange = onDbPassChange, label = "Hasło")
                    Spacer(modifier = Modifier.size(8.dp))
                    Button(onClick = {
                        showDebug = true
                        debugLog = "Rozpoczynanie testu dla: $dbHost..."
                        testDatabase(dbHost, dbPort, dbName, dbUser, dbPass) { success, msg ->
                            debugLog = if (success) "SUKCES: $msg" else "BŁĄD: $msg"
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Debuguj Baze") }
                }
            }
        }
        
        if (showDebug) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)).clickable { showDebug = false }, contentAlignment = Alignment.Center) {
                Column(modifier = Modifier.background(Color(0xFF2D2D2D), RoundedCornerShape(16.dp)).padding(24.dp).fillMaxWidth(0.7f)) {
                    Text("Logi MySQL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.size(16.dp))
                    Text(debugLog, color = if (debugLog.startsWith("SUKCES")) Color.Green else Color.Yellow, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.size(24.dp))
                    Button(onClick = { showDebug = false }, modifier = Modifier.align(Alignment.End)) { Text("Zamknij") }
                }
            }
        }
    }
}

fun testDatabase(host: String, port: String, name: String, user: String, pass: String, callback: (Boolean, String) -> Unit) {
    Log.d(TAG, "testDatabase start: $host:$port")
    Thread {
        try {
            Log.d(TAG, "Ładowanie sterownika...")
            Class.forName("com.mysql.jdbc.Driver")
            val url = "jdbc:mysql://$host:$port/$name?useSSL=false&connectTimeout=5000"
            DriverManager.getConnection(url, user, pass).use { _ ->
                Handler(Looper.getMainLooper()).post { callback(true, "Połączono pomyślnie z bazą $name") }
            }
        } catch (e: Throwable) {
            Handler(Looper.getMainLooper()).post { callback(false, e.localizedMessage ?: e.toString()) }
        }
    }.start()
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ModernTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) }, singleLine = true, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor, unfocusedBorderColor = onSurfaceColor.copy(alpha = 0.5f), focusedLabelColor = primaryColor, unfocusedLabelColor = onSurfaceColor.copy(alpha = 0.6f), focusedTextColor = onSurfaceColor, unfocusedTextColor = onSurfaceColor)
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerLabel(name: String, setsWon: Int, modifier: Modifier) {
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    Row(modifier = modifier.padding(32.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = name, style = MaterialTheme.typography.headlineLarge, color = onSurfaceColor)
        Box(modifier = Modifier.background(primaryColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)).border(BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f)), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 4.dp)) {
            Text(text = setsWon.toString(), style = MaterialTheme.typography.headlineSmall, color = primaryColor)
        }
    }
}

fun manageDatabaseMatch(dbConfig: Map<String, String>, sessionId: Long, player1: String, player2: String, scores: List<String>, winner: String? = null, action: String = "UPDATE") {
    Thread {
        try {
            Class.forName("com.mysql.jdbc.Driver")
            val url = "jdbc:mysql://${dbConfig["host"]}:${dbConfig["port"]}/${dbConfig["name"]}?useSSL=false"
            DriverManager.getConnection(url, dbConfig["user"], dbConfig["pass"]).use { conn ->
                val stmt = conn.createStatement()
                stmt.execute("CREATE TABLE IF NOT EXISTS active_matches (session_id BIGINT PRIMARY KEY, player1 VARCHAR(255), player2 VARCHAR(255), score_summary TEXT, match_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP)")
                stmt.execute("CREATE TABLE IF NOT EXISTS match_history (id INT AUTO_INCREMENT PRIMARY KEY, session_id BIGINT, player1 VARCHAR(255), player2 VARCHAR(255), score_summary TEXT, winner VARCHAR(255), match_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP)")

                when (action) {
                    "UPDATE" -> {
                        val sql = "INSERT INTO active_matches (session_id, player1, player2, score_summary) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE score_summary = VALUES(score_summary)"
                        conn.prepareStatement(sql).apply {
                            setLong(1, sessionId); setString(2, player1); setString(3, player2); setString(4, scores.joinToString(", "))
                            executeUpdate()
                        }
                    }
                    "FINISH" -> {
                        val del = "DELETE FROM active_matches WHERE session_id = ?"
                        conn.prepareStatement(del).apply { setLong(1, sessionId); executeUpdate() }
                        val ins = "INSERT INTO match_history (session_id, player1, player2, score_summary, winner) VALUES (?, ?, ?, ?, ?)"
                        conn.prepareStatement(ins).apply {
                            setLong(1, sessionId); setString(2, player1); setString(3, player2); setString(4, scores.joinToString(", ")); setString(5, winner ?: "Brak")
                            executeUpdate()
                        }
                    }
                    "DELETE" -> {
                        val del = "DELETE FROM active_matches WHERE session_id = ?"
                        conn.prepareStatement(del).apply { setLong(1, sessionId); executeUpdate() }
                    }
                }
            }
        } catch (e: Exception) { Log.e(TAG, "DB error ($action): ${e.message}") }
    }.start()
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DetailsScreen(navController: NavController, changeSidesEnabled: Boolean, changeSidesAnimationEnabled: Boolean, player1Name: String, player2Name: String, resume: Boolean = false, dbConfig: Map<String, String>?) {
    var leftCounter by remember { mutableIntStateOf(0) }
    var rightCounter by remember { mutableIntStateOf(0) }
    val history = remember { mutableStateListOf<String>() }
    val setsPointHistory = remember { mutableStateListOf<List<String>>() }
    val setScores = remember { mutableStateListOf<String>() }
    val focusRequester = remember { FocusRequester() }
    var startingPlayer by remember { mutableStateOf<Int?>(null) }
    var setStartingPlayer by remember { mutableStateOf<Int?>(null) }
    var servicePlayer by remember { mutableIntStateOf(1) }
    var serviceCount by remember { mutableIntStateOf(1) }
    var winner by remember { mutableStateOf<Int?>(null) }
    var matchSessionId by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showExitDialog by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val soundPlayer = remember { SoundPlayer(context) }

    LaunchedEffect(resume) {
        if (resume) {
            val prefs = context.getSharedPreferences("MatchProgress", Context.MODE_PRIVATE)
            if (prefs.getBoolean("exists", false)) {
                leftCounter = prefs.getInt("left", 0)
                rightCounter = prefs.getInt("right", 0)
                val h = prefs.getString("history", "") ?: ""
                if (h.isNotEmpty()) history.addAll(h.split(","))
                val s = prefs.getString("setScores", "") ?: ""
                if (s.isNotEmpty()) setScores.addAll(s.split(","))
                val sp = prefs.getInt("startingPlayer", 0)
                if (sp != 0) {
                    startingPlayer = sp
                    matchSessionId = prefs.getLong("sessionId", System.currentTimeMillis())
                    
                    var currentStart = startingPlayer!!
                    repeat(setScores.size) { currentStart = if (currentStart == 1) 2 else 1 }
                    setStartingPlayer = currentStart
                    
                    val isFinalSet = setScores.size == 4
                    val totalPoints = leftCounter + rightCounter
                    val deuceThreshold = if (isFinalSet) 5 else 10
                    val isDeuce = leftCounter >= deuceThreshold && rightCounter >= deuceThreshold
                    
                    if (isFinalSet || isDeuce) {
                        servicePlayer = if (totalPoints % 2 == 0) setStartingPlayer!! else (if (setStartingPlayer == 1) 2 else 1)
                        serviceCount = 1
                    } else {
                        val servicePairIndex = totalPoints / 2
                        servicePlayer = if (servicePairIndex % 2 == 0) setStartingPlayer!! else (if (setStartingPlayer == 1) 2 else 1)
                        serviceCount = (totalPoints % 2) + 1
                    }
                }
            }
        }
    }

    fun updateGameState(action: String = "UPDATE") {
        if (isFinished && action == "UPDATE") return
        saveMatchProgress(context, leftCounter, rightCounter, history.toList(), setScores.toList(), startingPlayer, player1Name, player2Name, matchSessionId)
        dbConfig?.let {
            val currentScores = setScores.toList() + "$leftCounter-$rightCounter"
            val winnerName = when (winner) {
                1 -> player1Name
                2 -> player2Name
                else -> null
            }
            manageDatabaseMatch(it, matchSessionId, player1Name, player2Name, currentScores, winnerName, action)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            soundPlayer.release()
            if (!isFinished) {
                // Remove from active matches in DB if match was not finished
                dbConfig?.let { manageDatabaseMatch(it, matchSessionId, player1Name, player2Name, emptyList(), null, "DELETE") }
            }
        }
    }

    val isSwapped = changeSidesEnabled && (setScores.size % 2 != 0)
    val p1SetsWon = setScores.count { it.contains("-") && it.split('-')[0].toInt() > it.split('-')[1].toInt() }
    val p2SetsWon = setScores.count { it.contains("-") && it.split('-')[1].toInt() > it.split('-')[0].toInt() }

    BackHandler { showExitDialog = true }

    if (showExitDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { androidx.compose.material3.Text("Przerwać mecz?") },
            text = { androidx.compose.material3.Text("Mecz zostanie zapisany lokalnie, ale usunięty z tabeli aktywnych meczów.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { navController.popBackStack() }) { androidx.compose.material3.Text("Tak") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showExitDialog = false }) { androidx.compose.material3.Text("Nie") }
            }
        )
    }

    when {
        winner != null -> {
            LaunchedEffect(winner) { 
                isFinished = true
                clearMatchProgress(context)
                updateGameState("FINISH")
            }
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                val winnerName = if (winner == 1) player1Name else player2Name
                Text("$winnerName wygrał!", style = MaterialTheme.typography.displayLarge); Spacer(modifier = Modifier.size(24.dp))
                Box(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp)).padding(24.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Podsumowanie meczu", style = MaterialTheme.typography.headlineSmall); Spacer(modifier = Modifier.size(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { setScores.forEach { score -> Text(text = score, style = MaterialTheme.typography.titleLarge) } }
                    }
                }
                Spacer(modifier = Modifier.size(40.dp))
                Button(onClick = { navController.popBackStack() }) { Text("Powrót do menu głównego") }
            }
        }
        startingPlayer == null -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Kto zaczyna serwis?", style = MaterialTheme.typography.displaySmall); Spacer(modifier = Modifier.size(40.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Button(onClick = { startingPlayer = 1; setStartingPlayer = 1; servicePlayer = 1; serviceCount = 1; updateGameState() }) { Text(player1Name, modifier = Modifier.padding(horizontal = 16.dp)) }
                    Button(onClick = { startingPlayer = 2; setStartingPlayer = 2; servicePlayer = 2; serviceCount = 1; updateGameState() }) { Text(player2Name, modifier = Modifier.padding(horizontal = 16.dp)) }
                }
            }
        }
        else -> {
            val isFinalSet = setScores.size == 4
            Box(modifier = Modifier.fillMaxSize().focusRequester(focusRequester).focusable().onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action != KeyEvent.ACTION_UP) return@onKeyEvent false
                val deuceThreshold = if (isFinalSet) 5 else 10
                
                when (keyEvent.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> { leftCounter++; history.add("left"); soundPlayer.playAddPointSound() }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> { rightCounter++; history.add("right"); soundPlayer.playAddPointSound() }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (history.isNotEmpty()) {
                            val lastPointFor = history.removeAt(history.lastIndex); if (lastPointFor == "left") leftCounter-- else rightCounter--; soundPlayer.playRemovePointSound()
                            val totalPoints = leftCounter + rightCounter
                            val isDeuceNow = leftCounter >= deuceThreshold && rightCounter >= deuceThreshold
                            if (isFinalSet || isDeuceNow) {
                                servicePlayer = if (totalPoints % 2 == 0) setStartingPlayer!! else (if (setStartingPlayer == 1) 2 else 1)
                                serviceCount = 1
                            } else {
                                val servicePairIndex = totalPoints / 2
                                servicePlayer = if (servicePairIndex % 2 == 0) setStartingPlayer!! else (if (setStartingPlayer == 1) 2 else 1)
                                serviceCount = (totalPoints % 2) + 1
                            }
                        } else if (setsPointHistory.isNotEmpty()) {
                            val lastSetHistory = setsPointHistory.removeAt(setsPointHistory.lastIndex); setScores.removeAt(setScores.lastIndex); history.clear(); history.addAll(lastSetHistory)
                            val prevLeftCounter = history.count { it == "left" }; val prevRightCounter = history.count { it == "right" }; val lastPointFor = history.removeAt(history.lastIndex); leftCounter = if (lastPointFor == "left") prevLeftCounter - 1 else prevLeftCounter; rightCounter = if (lastPointFor == "right") prevRightCounter - 1 else prevRightCounter; setStartingPlayer = if (setStartingPlayer == 1) 2 else 1; val totalPoints = leftCounter + rightCounter
                            val isDeuceNow = leftCounter >= deuceThreshold && rightCounter >= deuceThreshold
                            if (isFinalSet || isDeuceNow) { servicePlayer = if (totalPoints % 2 == 0) setStartingPlayer!! else (if (setStartingPlayer == 1) 2 else 1); serviceCount = 1 } else { val servicePairIndex = totalPoints / 2; servicePlayer = if (servicePairIndex % 2 == 0) setStartingPlayer!! else (if (setStartingPlayer == 1) 2 else 1); serviceCount = (totalPoints % 2) + 1 }; soundPlayer.playRemovePointSound()
                        }
                        updateGameState()
                        return@onKeyEvent true
                    }
                    else -> return@onKeyEvent false
                }
                val winPoints = if (isFinalSet) 6 else 11
                val setWon = (leftCounter >= winPoints && leftCounter >= rightCounter + 2) || (rightCounter >= winPoints && rightCounter >= leftCounter + 2)
                if (setWon && setScores.size < 5) {
                    val p1Score = if (isSwapped) rightCounter else leftCounter; val p2Score = if (isSwapped) leftCounter else rightCounter; setScores.add("$p1Score-$p2Score")
                    setsPointHistory.add(history.toList())
                    if (setScores.count { it.contains("-") && it.split('-')[0].toInt() > it.split('-')[1].toInt() } == 3) winner = 1 
                    else if (setScores.count { it.contains("-") && it.split('-')[1].toInt() > it.split('-')[0].toInt() } == 3) winner = 2 
                    else { leftCounter = 0; rightCounter = 0; history.clear(); setStartingPlayer = if (setStartingPlayer == 1) 2 else 1; servicePlayer = setStartingPlayer!!; serviceCount = 1 }
                } else if (!setWon) { 
                    val isDeuce = leftCounter >= deuceThreshold && rightCounter >= deuceThreshold
                    if (isFinalSet || isDeuce) { servicePlayer = if (servicePlayer == 1) 2 else 1; serviceCount = 1 } else { if (serviceCount == 2) { serviceCount = 1; servicePlayer = if (servicePlayer == 1) 2 else 1 } else serviceCount++ } 
                }
                updateGameState()
                true
            }) {
                // Animated Side Switching for Player Names
                Row(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
                    val p1Label = @Composable { PlayerLabel(name = player1Name, setsWon = p1SetsWon, modifier = Modifier) }
                    val p2Label = @Composable { PlayerLabel(name = player2Name, setsWon = p2SetsWon, modifier = Modifier) }
                    
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopStart) {
                        if (changeSidesAnimationEnabled) {
                            AnimatedContent(
                                targetState = isSwapped,
                                transitionSpec = {
                                    if (targetState) {
                                        (slideInHorizontally { it } + fadeIn(tween(500))).togetherWith(slideOutHorizontally { -it } + fadeOut(tween(500)))
                                    } else {
                                        (slideInHorizontally { -it } + fadeIn(tween(500))).togetherWith(slideOutHorizontally { it } + fadeOut(tween(500)))
                                    }
                                }
                            ) { swapped -> if (swapped) p2Label() else p1Label() }
                        } else {
                            if (isSwapped) p2Label() else p1Label()
                        }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopEnd) {
                        if (changeSidesAnimationEnabled) {
                            AnimatedContent(
                                targetState = isSwapped,
                                transitionSpec = {
                                    if (targetState) {
                                        (slideInHorizontally { -it } + fadeIn(tween(500))).togetherWith(slideOutHorizontally { it } + fadeOut(tween(500)))
                                    } else {
                                        (slideInHorizontally { it } + fadeIn(tween(500))).togetherWith(slideOutHorizontally { -it } + fadeOut(tween(500)))
                                    }
                                }
                            ) { swapped -> if (swapped) p1Label() else p2Label() }
                        } else {
                            if (isSwapped) p1Label() else p2Label()
                        }
                    }
                }

                // Center Score and Set Info
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Row(horizontalArrangement = Arrangement.spacedBy(120.dp), verticalAlignment = Alignment.CenterVertically) { 
                        Text(text = "$leftCounter", fontSize = 200.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = "$rightCounter", fontSize = 200.sp, color = MaterialTheme.colorScheme.onSurface) 
                    }
                    Box(modifier = Modifier.padding(top = 16.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)), RoundedCornerShape(12.dp)).padding(horizontal = 32.dp, vertical = 12.dp)) { 
                        Text(text = "SET ${setScores.size + 1}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) 
                    }
                }
                
                val isServiceOnLeft = if (!isSwapped) servicePlayer == 1 else servicePlayer == 2
                Box(Modifier.align(if (isServiceOnLeft) Alignment.BottomStart else Alignment.BottomEnd).padding(32.dp).size(28.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                if (serviceCount == 2 && !isFinalSet && !(leftCounter >= 10 && rightCounter >= 10)) Box(Modifier.align(if (isServiceOnLeft) Alignment.BottomEnd else Alignment.BottomStart).padding(32.dp).size(28.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape))
                
                // Set Scores Row
                Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    repeat(5) { index ->
                        val score = setScores.getOrNull(index) ?: "- : -"
                        val displayScore = if (isSwapped && score != "- : -") { 
                            val parts = score.split('-')
                            "${parts[1]} : ${parts[0]}" 
                        } else score
                        Text(text = displayScore, style = MaterialTheme.typography.titleMedium, color = if (score == "- : -") MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        }
    }
}
