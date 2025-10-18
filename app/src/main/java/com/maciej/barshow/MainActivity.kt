
package com.maciej.barshow

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.onKeyEvent
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
    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(onNavigateToDetails = { navController.navigate("details") })
        }
        composable("details") {
            DetailsScreen(navController = navController)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MainScreen(onNavigateToDetails: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Wybierz grę", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 24.dp))
            Button(onClick = onNavigateToDetails) {
                Text("Tenis Stołowy")
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DetailsScreen(navController: NavController) {
    var leftCounter by remember { mutableIntStateOf(0) }
    var rightCounter by remember { mutableIntStateOf(0) }
    val history = remember { mutableStateListOf<String>() }
    val setScores = remember { mutableStateListOf<String>() }
    val focusRequester = remember { FocusRequester() }

    var startingPlayer by remember { mutableStateOf<Int?>(null) }
    var setStartingPlayer by remember { mutableStateOf<Int?>(null) }
    var servicePlayer by remember { mutableIntStateOf(1) }
    var serviceCount by remember { mutableIntStateOf(1) }
    var winner by remember { mutableStateOf<Int?>(null) }

    when {
        winner != null -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Gracz $winner wygrał!", style = MaterialTheme.typography.displayMedium)

                Text(
                    text = "Podsumowanie:",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 24.dp)
                )
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    setScores.forEach { score ->
                        Text(text = score, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Button(onClick = { navController.popBackStack() }, modifier = Modifier.padding(top = 24.dp)) {
                    Text("Powrót do menu")
                }
            }
        }

        startingPlayer == null -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Kto zaczyna?", style = MaterialTheme.typography.headlineMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(top = 20.dp)) {
                    Button(onClick = {
                        startingPlayer = 1
                        setStartingPlayer = 1
                        servicePlayer = 1
                        serviceCount = 1
                    }) {
                        Text("Gracz 1")
                    }
                    Button(onClick = {
                        startingPlayer = 2
                        setStartingPlayer = 2
                        servicePlayer = 2
                        serviceCount = 1
                    }) {
                        Text("Gracz 2")
                    }
                }
            }
        }

        else -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .focusable()
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.nativeKeyEvent.action != KeyEvent.ACTION_UP) return@onKeyEvent false

                        val isFinalSet = setScores.size == 4
                        var pointScored = false

                        when (keyEvent.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                leftCounter++
                                history.add("left")
                                pointScored = true
                            }

                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                rightCounter++
                                history.add("right")
                                pointScored = true
                            }

                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                if (history.isNotEmpty()) {
                                    val lastPointFor = history.removeLast()
                                    val wasPointInThisSet = when (lastPointFor) {
                                        "left" -> if (leftCounter > 0) { leftCounter--; true } else false
                                        "right" -> if (rightCounter > 0) { rightCounter--; true } else false
                                        else -> false
                                    }

                                    if (wasPointInThisSet) {
                                        if (isFinalSet) {
                                            servicePlayer = if (servicePlayer == 1) 2 else 1
                                        } else {
                                            serviceCount--
                                            if (serviceCount == 0) {
                                                servicePlayer = if (servicePlayer == 1) 2 else 1
                                                serviceCount = 2
                                            }
                                        }
                                    }
                                }
                            }

                            else -> return@onKeyEvent false
                        }

                        if (pointScored) {
                            val winPoints = if (isFinalSet) 6 else 11
                            val setWon = (leftCounter >= winPoints && leftCounter >= rightCounter + 2) ||
                                    (rightCounter >= winPoints && rightCounter >= leftCounter + 2)

                            if (setWon && setScores.size < 5) {
                                setScores.add("$leftCounter-$rightCounter")

                                val leftSetsWon = setScores.count { it.split('-')[0].toInt() > it.split('-')[1].toInt() }
                                val rightSetsWon = setScores.count { it.split('-')[1].toInt() > it.split('-')[0].toInt() }

                                if (leftSetsWon == 3) {
                                    winner = 1
                                } else if (rightSetsWon == 3) {
                                    winner = 2
                                } else {
                                    leftCounter = 0
                                    rightCounter = 0
                                    setStartingPlayer = if (setStartingPlayer == 1) 2 else 1
                                    servicePlayer = setStartingPlayer!!
                                    serviceCount = 1
                                }
                            } else if (!setWon) {
                                if (isFinalSet) {
                                    servicePlayer = if (servicePlayer == 1) 2 else 1
                                } else {
                                    if (serviceCount == 2) {
                                        serviceCount = 1
                                        servicePlayer = if (servicePlayer == 1) 2 else 1
                                    } else {
                                        serviceCount++
                                    }
                                }
                            }
                        }
                        true
                    }
            ) {
                // UI dla gry w toku
                Text(
                    text = "Gracz 1",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.align(Alignment.TopStart).padding(32.dp)
                )
                Text(
                    text = "Gracz 2",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.align(Alignment.TopEnd).padding(32.dp)
                )
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(100.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "$leftCounter", fontSize = 120.sp)
                    Text(text = "$rightCounter", fontSize = 120.sp)
                }
                Box(
                    Modifier
                        .align(if (servicePlayer == 1) Alignment.BottomStart else Alignment.BottomEnd)
                        .padding(32.dp)
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    repeat(5) { index ->
                        val score = setScores.getOrNull(index) ?: "- : -"
                        Text(
                            text = score,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
    }
}
