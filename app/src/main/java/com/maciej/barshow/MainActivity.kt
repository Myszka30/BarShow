
package com.maciej.barshow

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.maciej.barshow.ui.theme.BarShowTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BarShowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    MyApp()
                }
            }
        }
    }
}

@Composable
fun MyApp() {
    val navController = rememberNavController()
    var changeSidesEnabled by remember { mutableStateOf(true) }
    var player1Name by remember { mutableStateOf("Gracz 1") }
    var player2Name by remember { mutableStateOf("Gracz 2") }

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                onNavigateToDetails = { navController.navigate("details") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("details") {
            DetailsScreen(
                navController = navController,
                changeSidesEnabled = changeSidesEnabled,
                player1Name = player1Name,
                player2Name = player2Name
            )
        }
        composable("settings") {
            SettingsScreen(
                navController = navController,
                changeSidesEnabled = changeSidesEnabled,
                onToggleChangeSides = { changeSidesEnabled = it },
                player1Name = player1Name,
                onPlayer1NameChange = { player1Name = it },
                player2Name = player2Name,
                onPlayer2NameChange = { player2Name = it }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MainScreen(onNavigateToDetails: () -> Unit, onNavigateToSettings: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Wybierz grę",
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // Przycisk 1: Zmniejszony i wyśrodkowany
            Button(
                onClick = onNavigateToDetails,
                modifier = Modifier.size(width = 220.dp, height = 50.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tenis Stołowy", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.size(24.dp))

            // Przycisk 2: Zmniejszony i wyśrodkowany
            Button(
                onClick = onNavigateToSettings,
                modifier = Modifier.size(width = 220.dp, height = 50.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Ustawienia", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}


@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    changeSidesEnabled: Boolean,
    onToggleChangeSides: (Boolean) -> Unit,
    player1Name: String,
    onPlayer1NameChange: (String) -> Unit,
    player2Name: String,
    onPlayer2NameChange: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("Ustawienia", style = MaterialTheme.typography.displaySmall)
            Spacer(modifier = Modifier.size(40.dp))

            Column(modifier = Modifier.fillMaxWidth(0.5f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text("Zmiana stron po secie", style = MaterialTheme.typography.titleMedium)
                    Switch(checked = changeSidesEnabled, onCheckedChange = onToggleChangeSides)
                }

                ModernTextField(value = player1Name, onValueChange = onPlayer1NameChange, label = "Nazwa Gracza 1")
                ModernTextField(value = player2Name, onValueChange = onPlayer2NameChange, label = "Nazwa Gracza 2")
            }

            Spacer(modifier = Modifier.size(48.dp))
            Button(onClick = { navController.popBackStack() }) {
                Text("Zapisz i powróć")
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ModernTextField(value: String, onValueChange: (String) -> Unit, label: String) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = primaryColor,
            unfocusedBorderColor = onSurfaceColor.copy(alpha = 0.5f),
            focusedLabelColor = primaryColor,
            unfocusedLabelColor = onSurfaceColor.copy(alpha = 0.6f),
            focusedTextColor = onSurfaceColor,
            unfocusedTextColor = onSurfaceColor
        )
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerLabel(name: String, setsWon: Int, modifier: Modifier) {
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier.padding(32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.headlineLarge,
            color = onSurfaceColor
        )
        Box(
            modifier = Modifier
                .background(primaryColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = setsWon.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = primaryColor
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DetailsScreen(navController: NavController, changeSidesEnabled: Boolean, player1Name: String, player2Name: String) {
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

    val context = LocalContext.current
    val soundPlayer = remember { SoundPlayer(context) }

    DisposableEffect(Unit) {
        onDispose {
            soundPlayer.release()
        }
    }

    val isSwapped = changeSidesEnabled && (setScores.size % 2 != 0)
    val p1SetsWon = setScores.count { it.split('-')[0].toInt() > it.split('-')[1].toInt() }
    val p2SetsWon = setScores.count { it.split('-')[1].toInt() > it.split('-')[0].toInt() }

    when {
        winner != null -> {
            val winnerName = if (winner == 1) player1Name else player2Name
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("$winnerName wygrał!", style = MaterialTheme.typography.displayLarge)
                Spacer(modifier = Modifier.size(24.dp))
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Podsumowanie meczu", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.size(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            setScores.forEach { score ->
                                Text(text = score, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.size(40.dp))
                Button(onClick = { navController.popBackStack() }) {
                    Text("Powrót do menu głównego")
                }
            }
        }

        startingPlayer == null -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Kto zaczyna serwis?", style = MaterialTheme.typography.displaySmall)
                Spacer(modifier = Modifier.size(40.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Button(onClick = { startingPlayer = 1; setStartingPlayer = 1; servicePlayer = 1; serviceCount = 1 }) { Text(player1Name, modifier = Modifier.padding(horizontal = 16.dp)) }
                    Button(onClick = { startingPlayer = 2; setStartingPlayer = 2; servicePlayer = 2; serviceCount = 1 }) { Text(player2Name, modifier = Modifier.padding(horizontal = 16.dp)) }
                }
            }
        }

        else -> {
            val isFinalSet = setScores.size == 4
            Box(
                modifier = Modifier.fillMaxSize().focusRequester(focusRequester).focusable().onKeyEvent { keyEvent ->
                    if (keyEvent.nativeKeyEvent.action != KeyEvent.ACTION_UP) return@onKeyEvent false
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT -> { leftCounter++; history.add("left"); soundPlayer.playAddPointSound() }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> { rightCounter++; history.add("right"); soundPlayer.playAddPointSound() }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (history.isNotEmpty()) {
                                val lastPointFor = history.removeAt(history.lastIndex)
                                if (lastPointFor == "left") leftCounter-- else rightCounter--
                                soundPlayer.playRemovePointSound()
                                if (isFinalSet) servicePlayer = if (servicePlayer == 1) 2 else 1
                                else { serviceCount--; if (serviceCount == 0) { servicePlayer = if (servicePlayer == 1) 2 else 1; serviceCount = 2 } }
                            } else if (setsPointHistory.isNotEmpty()) {
                                val lastSetHistory = setsPointHistory.removeAt(setsPointHistory.lastIndex)
                                setScores.removeAt(setScores.lastIndex)
                                history.clear(); history.addAll(lastSetHistory)
                                val prevLeftCounter = history.count { it == "left" }; val prevRightCounter = history.count { it == "right" }
                                val lastPointFor = history.removeAt(history.lastIndex)
                                leftCounter = if (lastPointFor == "left") prevLeftCounter - 1 else prevLeftCounter
                                rightCounter = if (lastPointFor == "right") prevRightCounter - 1 else prevRightCounter
                                setStartingPlayer = if (setStartingPlayer == 1) 2 else 1
                                val isNowFinalSet = setScores.size == 4
                                val totalPoints = leftCounter + rightCounter
                                if (isNowFinalSet) { servicePlayer = if (totalPoints % 2 == 0) setStartingPlayer!! else (if (setStartingPlayer == 1) 2 else 1); serviceCount = 1 }
                                else { val servicePairIndex = totalPoints / 2; servicePlayer = if (servicePairIndex % 2 == 0) setStartingPlayer!! else (if (setStartingPlayer == 1) 2 else 1); serviceCount = (totalPoints % 2) + 1 }
                                soundPlayer.playRemovePointSound()
                            }
                            return@onKeyEvent true
                        }
                        else -> return@onKeyEvent false
                    }
                    val winPoints = if (isFinalSet) 6 else 11
                    val setWon = (leftCounter >= winPoints && leftCounter >= rightCounter + 2) || (rightCounter >= winPoints && rightCounter >= leftCounter + 2)
                    if (setWon && setScores.size < 5) {
                        val p1Score = if (isSwapped) rightCounter else leftCounter
                        val p2Score = if (isSwapped) leftCounter else rightCounter
                        setScores.add("$p1Score-$p2Score")
                        setsPointHistory.add(history.toList())
                        val currentP1Sets = setScores.count { it.split('-')[0].toInt() > it.split('-')[1].toInt() }
                        val currentP2Sets = setScores.count { it.split('-')[1].toInt() > it.split('-')[0].toInt() }
                        if (currentP1Sets == 3) winner = 1 else if (currentP2Sets == 3) winner = 2
                        else { leftCounter = 0; rightCounter = 0; history.clear(); setStartingPlayer = if (setStartingPlayer == 1) 2 else 1; servicePlayer = setStartingPlayer!!; serviceCount = 1 }
                    } else if (!setWon) {
                        if (isFinalSet) servicePlayer = if (servicePlayer == 1) 2 else 1
                        else { if (serviceCount == 2) { serviceCount = 1; servicePlayer = if (servicePlayer == 1) 2 else 1 } else serviceCount++ }
                    }
                    true
                }
            ) {
                PlayerLabel(
                    name = if (isSwapped) player2Name else player1Name,
                    setsWon = if (isSwapped) p2SetsWon else p1SetsWon,
                    modifier = Modifier.align(Alignment.TopStart)
                )
                PlayerLabel(
                    name = if (isSwapped) player1Name else player2Name,
                    setsWon = if (isSwapped) p1SetsWon else p2SetsWon,
                    modifier = Modifier.align(Alignment.TopEnd)
                )

                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Row(horizontalArrangement = Arrangement.spacedBy(120.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "$leftCounter", fontSize = 200.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = "$rightCounter", fontSize = 200.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Box(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)), RoundedCornerShape(12.dp))
                            .padding(horizontal = 32.dp, vertical = 12.dp)
                    ) {
                        Text(text = "SET ${setScores.size + 1}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }

                val isServiceOnLeft = if (!isSwapped) servicePlayer == 1 else servicePlayer == 2
                Box(Modifier.align(if (isServiceOnLeft) Alignment.BottomStart else Alignment.BottomEnd).padding(32.dp).size(28.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                if (serviceCount == 2 && !isFinalSet) Box(Modifier.align(if (isServiceOnLeft) Alignment.BottomEnd else Alignment.BottomStart).padding(32.dp).size(28.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape))

                Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    repeat(5) { index ->
                        val score = setScores.getOrNull(index) ?: "- : -"
                        val displayScore = if (isSwapped && score != "- : -") {
                            val parts = score.split('-'); "${parts[1]} : ${parts[0]}"
                        } else score
                        Text(text = displayScore, style = MaterialTheme.typography.titleMedium, color = if (score == "- : -") MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        }
    }
}
