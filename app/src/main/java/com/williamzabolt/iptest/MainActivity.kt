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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
        setContent {
            IpTestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    val context = LocalContext.current
                    val devices = remember {
                        mutableStateListOf<DeviceInfo>()
                    }
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
                                        getIps {
                                            devices.add(it)
                                        }
                                    })
                            }) {
                            Text(text = "Ver quem está na rede")
                        }
                        ShowList(
                            items = devices,
                            myDevice = DeviceInfo("", "")
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
                    text = item.hostname,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Text(
                    modifier = Modifier.padding(top = 10.dp),
                    text = item.ip,
                    fontSize = 16.sp,
                )
                item.mac?.let {
                    Text(
                        modifier = Modifier.padding(top = 10.dp),
                        text = item.mac,
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }

    @Composable
    fun ShowList(items: List<DeviceInfo>, myDevice: DeviceInfo) {
        LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
            item {
                ItemRow(item = myDevice)
            }

            items(items) { item ->
                ItemRow(item = item)
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
                    // getIps()
                } else {
                    // permissão negada
                }
            }
        }
    }

    private fun getIps(
        newDevice: (DeviceInfo) -> Unit
    ) {
        val context = this@MainActivity
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val ipv4 = getIPv4Address(context)
                if (ipv4?.isNotEmpty() == true) {
                    val baseIP = ipv4.substring(0, ipv4.lastIndexOf('.') + 1)


                    val devicesInfo = scanNetwork(
                        baseIP = baseIP
                    ) {
                        newDevice(it)
                    }
                    /* if (devicesInfo.isNotEmpty()) {
                         text.value = devicesInfo.toString()
                     } else {
                         val devicesByArp = scanNetworkByArp(baseIP)
                         if (devicesByArp.isNotEmpty()) {
                             text.value = devicesByArp.toString()
                         } else {
                             text.value = "Erro ao obter endereços da rede do IP $baseIP"
                         }
                     }*/
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