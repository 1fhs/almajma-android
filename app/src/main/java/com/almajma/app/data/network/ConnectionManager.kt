package com.almajma.app.data.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class NetworkMode {
    ONLINE,          // Full central servers API active
    ZERO_DATA,       // Text only, compressed payload headers, blocked assets
    OFFLINE_MESH     // Offline local Wi-Fi Aware / BLE mesh (local token signed & cached)
}

class ConnectionManager {
    private val _networkMode = MutableStateFlow(NetworkMode.ONLINE)
    val networkMode: StateFlow<NetworkMode> = _networkMode.asStateFlow()

    private val _latencyMs = MutableStateFlow(42)
    val latencyMs: StateFlow<Int> = _latencyMs.asStateFlow()

    private val _meshPeersNearby = MutableStateFlow(0)
    val meshPeersNearby: StateFlow<Int> = _meshPeersNearby.asStateFlow()

    fun setMode(mode: NetworkMode) {
        _networkMode.value = mode
        when (mode) {
            NetworkMode.ONLINE -> {
                _latencyMs.value = (30..80).random()
                _meshPeersNearby.value = 0
            }
            NetworkMode.ZERO_DATA -> {
                _latencyMs.value = (250..600).random() // higher latency compression overhead
                _meshPeersNearby.value = 0
            }
            NetworkMode.OFFLINE_MESH -> {
                _latencyMs.value = 5 // local WiFi P2P micro-latency
                _meshPeersNearby.value = (2..5).random() // Simulate local taxi/moped drivers or pharmacies nearby 
            }
        }
    }

    /**
     * Generates a peer-to-peer cryptographic signature token for Mesh Signatures.
     */
    fun generateMeshToken(transactionId: String, amount: Double): String {
        val uniquePayload = "$transactionId:$amount:${System.currentTimeMillis()}:${UUID.randomUUID().toString().take(6)}"
        // Simulate local signature
        val hash = uniquePayload.hashCode().toString(16).uppercase()
        return "MESH-SIG-$hash"
    }

    /**
     * Compresses payload header for Zero-Rating data-saver mode simulation.
     */
    fun compressDataSaverRequest(payload: String): String {
        return "COMPRESSED-HDR|LEN:${payload.length}|TEXT_ONLY|${payload.take(60)}"
    }
}
