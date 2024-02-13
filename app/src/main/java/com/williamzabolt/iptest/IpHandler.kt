package com.williamzabolt.iptest

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.text.format.Formatter.formatIpAddress
import kotlinx.coroutines.runBlocking
import oshi.SystemInfo
import oshi.hardware.HardwareAbstractionLayer
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.Enumeration
import java.util.regex.Matcher
import java.util.regex.Pattern

fun getInterfaceIpAddress(
    defaultInterface: String
): DeviceInfo? {
    try {
        val command = Runtime.getRuntime().exec("ip addr show")
        val reader = BufferedReader(InputStreamReader(command.inputStream))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            if (line?.contains(defaultInterface) == true) {
                val nextLine = reader.readLine()
                val ipLine = reader.readLine()

                if (ipLine != null && nextLine != null) {
                    return DeviceInfo(
                        ip = extractIPAddress(ipLine)
                    )
                }
            }
        }
    } catch (ex: Exception) {
        return null
    }
    return null
}

private fun getDefaultInterface(
    nextFunction: (defaultInterface: String) -> DeviceInfo?
): DeviceInfo? {
    try {
        val interfaces: Enumeration<NetworkInterface>? = NetworkInterface.getNetworkInterfaces()
        if (interfaces != null) {
            while (interfaces.hasMoreElements()) {
                val iFace: NetworkInterface = interfaces.nextElement()
                val addresses: Enumeration<InetAddress> = iFace.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address: InetAddress = addresses.nextElement()
                    if (!address.isLoopbackAddress && !address.isLinkLocalAddress && address.isSiteLocalAddress) {
                        return DeviceInfo(
                            ip = address.hostAddress,
                            hostname = address.hostName
                        )
                    }
                }
            }
        }
        return nextFunction.invoke(
            getDefaultInterfaceByIpRoute() ?: "wlan0"
        )
    } catch (e: SocketException) {
        return nextFunction.invoke(
            getDefaultInterfaceByIpRoute() ?: "wlan0"
        )
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

fun getIPv4Address(context: Context): DeviceInfo? {
    return getIPAddressByWifiConnection(
        context = context,
        nextFunction = {
            getDefaultInterface { defaultInterface ->
                getInterfaceIpAddress(
                    defaultInterface = defaultInterface
                )
            }
        }
    )

}


fun getIPAddressByWifiConnection(
    context: Context,
    nextFunction: () -> DeviceInfo?
): DeviceInfo? {
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
                val ip = formatIpAddress(wifiInfo.ipAddress)
                return DeviceInfo(
                    ip = ip,
                    hostname = InetAddress.getByName(ip).hostName,
                    mac = wifiInfo.macAddress
                )
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

data class DeviceInfo(
    val hostname: String? = null,
    val ip: String?,
    val mac: String? = null,
    val operationalSystem: String? = null
)

// Função para varrer a rede e obter IPs ativos
fun scanNetwork(
    baseIP: String,
    hostIp: String,
    newDevice: (DeviceInfo) -> Unit
) {
    val lastIpNumber = hostIp.substring(hostIp.lastIndexOf('.') + 1).toInt()
    val validRange = (1..254).filter { it != lastIpNumber }
    for (i in validRange) {
        searchIp("$baseIP$i",
            newDevice = {
                newDevice(it)
            })
    }
}

private fun searchIp(
    targetIP: String,
    newDevice: (DeviceInfo) -> Unit
) {
    val address = InetAddress.getByName(targetIP)
    if (address.isReachable(300)) {
        val hostname = address.hostName
        val mac = getMacAddress(targetIP)
        val operationalSystem = getOsInfo(targetIP)
        val deviceInfo = DeviceInfo(
            hostname = hostname,
            ip = targetIP,
            mac = mac,
            operationalSystem = operationalSystem
        )
        newDevice(deviceInfo)
    }
}

fun getOsInfo(targetIP: String): String? {
    try {
        val command = "nmap -O $targetIP"
        val process = Runtime.getRuntime().exec(command)
        process.waitFor()

        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val result = StringBuilder()
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            result.append(line).append("\n")
        }

        return result.toString()
    } catch (e: Exception) {
        return null
    }
}

fun scanNetworkByArp(baseIP: String): List<DeviceInfo> = runBlocking {
    val discoveredDevices = mutableListOf<DeviceInfo>()
    for (i in 1..255) {
        val targetIP = "$baseIP$i"
        val command = "arp -a $targetIP"
        try {
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String? = reader.readLine()
            while (line != null) {
                if (line.contains("dynamic")) {
                    val parts = line.split("\\s+".toRegex())
                    val mac = parts[1]
                    val hostname = parts[0]
                    val deviceInfo = DeviceInfo(mac, hostname, targetIP)
                    discoveredDevices.add(deviceInfo)
                    break
                }
                line = reader.readLine()
            }
            process.waitFor()
            process.destroy()
        } catch (e: Exception) {
            println(e.message)
        }
    }

    return@runBlocking discoveredDevices
}

fun getMacAddress(ip: String): String? {
    try {
        val si = SystemInfo()
        val hal: HardwareAbstractionLayer = si.hardware

        val networkIFs = hal.networkIFs
        for (networkIF in networkIFs) {
            val addresses = networkIF.iPv4addr
            if (addresses.contains(ip)) {
                return networkIF.macaddr
            }
        }
    } catch (ex: Exception) {
        return null
    }
    return null
}