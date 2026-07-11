package com.example.crash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.system.exitProcess

class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val crashText = CrashHandler.getLastCrash(this) ?: "No crash details found."

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(color = Color(0xFF0A0A0A)) {
                    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                        Text(
                            text = "App crashed",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE57373)
                        )
                        Text(
                            text = "Copy this and send it so it can be fixed:",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color(0xFF161616))
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(text = crashText, fontSize = 11.sp, color = Color(0xFFCCCCCC))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Crash log", crashText))
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Copy to Clipboard") }

                            OutlinedButton(
                                onClick = {
                                    CrashHandler.clearLastCrash(this@CrashActivity)
                                    finishAffinity()
                                    exitProcess(0)
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Close App") }
                        }
                    }
                }
            }
        }
    }
}
