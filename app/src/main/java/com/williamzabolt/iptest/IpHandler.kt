package com.williamzabolt.iptest

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.text.format.Formatter.formatIpAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.SocketException
import java.net.UnknownHostException
import java.util.Enumeration
import java.util.regex.Matcher
import java.util.regex.Pattern

fun getInterfaceIpAddress(
    defaultInterface: String,
    nextFunction: () -> String?
): String? {
    try {
        val command = Runtime.getRuntime().exec("ip addr show")
        val reader = BufferedReader(InputStreamReader(command.inputStream))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            if (line?.contains(defaultInterface) == true) {
                val nextLine = reader.readLine()
                val ipLine = reader.readLine()

                if (ipLine != null && nextLine != null) {
                    return extractIPAddress(ipLine)
                }
            }
        }
    } catch (ex: Exception) {
        return nextFunction.invoke()
    }
    return nextFunction.invoke()
}

private fun getDefaultInterface(): String? {
    try {
        val interfaces: Enumeration<NetworkInterface>? = NetworkInterface.getNetworkInterfaces()
        if (interfaces != null) {
            while (interfaces.hasMoreElements()) {
                val iFace: NetworkInterface = interfaces.nextElement()
                val addresses: Enumeration<InetAddress> = iFace.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address: InetAddress = addresses.nextElement()
                    if (!address.isLoopbackAddress && !address.isLinkLocalAddress && address.isSiteLocalAddress) {
                        return iFace.name
                    }
                }
            }
        }
        return null
    } catch (e: SocketException) {
        return null
    }
}


private fun extractIPAddress(input: String?): String? {
    return try {
        if (input != null) {
            val regex = "\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b"
            val pattern: Pattern = Pattern.compile(regex)
            val matcher: Matcher = pattern.matcher(input)
            if (matcher.find()) {
                matcher.group()
            } else null
        } else null
    } catch (ex: Exception) {
        null
    }
}

fun getIPv4Address(context: Context): String? {
    val defaultInterface = getDefaultInterface() ?: getDefaultInterfaceByIpRoute() ?: "wlan0"
    return getIPAddressByWifiConnection(
        context = context,
        nextFunction = {
            getInterfaceIpAddress(
                defaultInterface = defaultInterface,
                nextFunction = {
                    getIpByInet(defaultInterface)
                }
            )
        }
    )
}

fun getIpByInet(defaultInterface: String): String? {
    return try {
        val inetAddress = InetAddress.getByName(defaultInterface)
        val hostAddress = inetAddress.hostAddress
        hostAddress
    } catch (ex: Exception) {
        null
    }
}


fun getIPAddressByWifiConnection(
    context: Context,
    nextFunction: () -> String?
): String? {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val connectivityManager = context.getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                val wifiManager =
                    context.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
                val wifiInfo = wifiManager.connectionInfo
                return formatIpAddress(wifiInfo.ipAddress)
            }
        } else {
            return nextFunction.invoke()
        }
    } catch (ex: Exception) {
        return nextFunction.invoke()
    }
    return nextFunction.invoke()
}

fun getDefaultInterfaceByIpRoute(): String? {
    try {
        val process = Runtime.getRuntime().exec("ip route get 8.8.8.8")
        val reader = BufferedReader(InputStreamReader(process.inputStream))

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            if (line?.contains("dev") == true) {
                val parts = line?.split(" ")
                if (parts != null && parts.size >= 2) {
                    return parts[parts.size - 1]
                }
            }
        }
    } catch (e: Exception) {
        return null
    }
    return null
}

// Função para varrer a rede e obter IPs ativos
fun scanNetwork(baseIP: String, callback: (List<String>) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        val discoveredIPs = mutableListOf<String>()

        for (i in 1..255) {
            val targetIP = "$baseIP$i"

            try {
                val socket = Socket(targetIP, 80)
                socket.close()
                discoveredIPs.add(targetIP)
            } catch (e: UnknownHostException) {
                print(e.message)
            } catch (e: Exception) {
                print(e.message)
            }
        }

        launch(Dispatchers.Main) {
            callback(discoveredIPs)
        }
    }
}