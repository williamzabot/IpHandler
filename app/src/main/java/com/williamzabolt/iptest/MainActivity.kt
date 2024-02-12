package com.williamzabolt.iptest

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.williamzabolt.iptest.ui.theme.IpTestTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    val text = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IpTestTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    val context = LocalContext.current
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = {
                                requestPermission(
                                    context = context,
                                    permissionGranted = {
                                        getIps()
                                    })
                            }) {
                            Text(text = "Ver quem está na rede")
                        }

                        Text(
                            modifier = Modifier.padding(top = 20.dp),
                            text = text.value
                        )
                    }

                }
            }
        }
    }

    private fun requestPermission(context: Context, permissionGranted: () -> Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.INTERNET
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE),
                PERMISSIONS_REQUEST_INTERNET
            )
        } else {
            permissionGranted()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray

    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_INTERNET -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getIps()
                } else {
                    // permissão negada
                }
            }
        }
    }

    private fun getIps() {
        val context = this@MainActivity
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val wlan0IP = getIPv4Address(context)
                if (wlan0IP?.isNotEmpty() == true) {
                    val baseIP = wlan0IP.substring(0, wlan0IP.lastIndexOf('.') + 1)
                    scanNetwork(baseIP) { discoveredIPs ->
                        text.value = discoveredIPs.toString()
                    }
                } else {
                    text.value = "Erro ao obter o endereço IPv4 do device"
                }

            } catch (e: Exception) {
                println(e.message)
                // Lida com exceções, se necessário
            }
        }
    }

    companion object {
        const val PERMISSIONS_REQUEST_INTERNET = 1
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    IpTestTheme {
        Button(onClick = {}) {
            Text(text = "Ver quem está na rede")

        }
    }
}