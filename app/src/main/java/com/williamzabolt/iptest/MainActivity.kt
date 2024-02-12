package com.williamzabolt.iptest

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.williamzabolt.iptest.ui.theme.IpTestTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermission(this)
        setContent {
            IpTestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    val devices = remember {
                        mutableStateListOf<DeviceInfo>()
                    }
                    val thisDevice = remember {
                        mutableStateOf<DeviceInfo?>(null)
                    }
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = {
                                getIps(
                                    newDevice = {
                                        devices.add(it)
                                    },
                                    getMyDevice = {
                                        thisDevice.value = it
                                    })
                            }) {
                            Text(text = "Ver quem está na rede")
                        }
                        ShowList(
                            items = devices,
                            myDevice = thisDevice.value
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun ItemRow(
        item: DeviceInfo
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 10.dp,
                    start = 10.dp
                )
        ) {
            Column {
                Text(
                    text = item.hostname ?: "Genérico",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                item.ip?.let {
                    Text(
                        modifier = Modifier.padding(top = 10.dp),
                        text = item.ip,
                        fontSize = 16.sp,
                    )
                }

                item.mac?.let {
                    Text(
                        modifier = Modifier.padding(top = 10.dp),
                        text = item.mac,
                        fontSize = 16.sp,
                    )
                }

                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)

                )
            }
        }
    }

    @Composable
    fun ShowList(items: List<DeviceInfo>, myDevice: DeviceInfo?) {
        LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
            myDevice?.let {
                item {
                    Text(
                        modifier = Modifier.padding(
                            start = 10.dp
                        ),
                        text = "Este device"
                    )
                    ItemRow(item = myDevice)
                }
            }

            items(items) { item ->
                ItemRow(item = item)
            }
        }
    }

    private fun requestPermission(context: Context) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.INTERNET
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(
                    Manifest.permission.INTERNET,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                PERMISSIONS_REQUEST_INTERNET
            )
        }
    }


    private fun getIps(
        newDevice: (DeviceInfo) -> Unit,
        getMyDevice: (DeviceInfo?) -> Unit
    ) {
        val context = this@MainActivity
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val ipv4 = getIPv4Address(context)
                getMyDevice(ipv4)
                val ip = ipv4?.ip
                if (ip?.isNotEmpty() == true) {
                    scanNetwork(
                        baseIP = ip.substring(0, ip.lastIndexOf('.') + 1)
                    ) {
                        newDevice(it)
                    }
                }

            } catch (e: Exception) {
                println(e.message)
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