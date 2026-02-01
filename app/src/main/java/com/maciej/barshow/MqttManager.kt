package com.maciej.barshow

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.nio.charset.StandardCharsets

private const val TAG = "MqttManager"

class MqttManager {
    private var client: MqttClient? = null
    private val _mqttEvents = MutableSharedFlow<MqttEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val mqttEvents: SharedFlow<MqttEvent> = _mqttEvents

    suspend fun connect(serverUri: String, clientId: String, user: String, pass: String, topicPrefix: String): Boolean = withContext(Dispatchers.IO) {
        disconnect()
        try {
            val persistence = MemoryPersistence()
            client = MqttClient(serverUri, clientId, persistence)
            
            val cleanTopic = if (topicPrefix.endsWith("/")) topicPrefix else "$topicPrefix/"
            
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                if (user.isNotEmpty()) userName = user
                if (pass.isNotEmpty()) password = pass.toCharArray()
                connectionTimeout = 10
                keepAliveInterval = 20
                isAutomaticReconnect = true
                
                // --- LAST WILL AND TESTAMENT ---
                // Jeśli aplikacja zostanie nagle zamknięta, broker wyśle "false" do match_active
                setWill("${cleanTopic}status/match_active", "false".toByteArray(), 1, false)
            }

            client?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.d(TAG, "Connection lost: ${cause?.message}")
                }
                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    if (topic == null || message == null) return
                    val payload = String(message.payload, StandardCharsets.UTF_8)
                    handleMessage(topic, payload, topicPrefix)
                }
                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

            client?.connect(options)
            subscribeToTopics(topicPrefix)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting: ${e.message}")
            false
        }
    }

    private fun subscribeToTopics(prefix: String) {
        try {
            val cleanPrefix = if (prefix.endsWith("/")) prefix else "$prefix/"
            client?.subscribe("${cleanPrefix}control/#", 1)
        } catch (e: Exception) {
            Log.e(TAG, "Error subscribing: ${e.message}")
        }
    }

    suspend fun publish(topic: String, message: String) = withContext(Dispatchers.IO) {
        try {
            if (client?.isConnected == true) {
                val mqttMessage = MqttMessage(message.toByteArray(StandardCharsets.UTF_8))
                mqttMessage.qos = 1
                client?.publish(topic, mqttMessage)
                true
            } else { false }
        } catch (e: Exception) {
            Log.e(TAG, "Error publishing: ${e.message}")
            false
        }
    }

    private fun handleMessage(topic: String, payload: String, prefix: String) {
        val cleanPrefix = if (prefix.endsWith("/")) prefix else "$prefix/"
        val subTopic = topic.removePrefix(cleanPrefix).removePrefix("control/")

        val event = when (subTopic) {
            "point_left" -> MqttEvent.PointLeft
            "point_right" -> MqttEvent.PointRight
            "remove_point" -> MqttEvent.RemovePoint
            "p1_name" -> MqttEvent.SetP1Name(payload)
            "p2_name" -> MqttEvent.SetP2Name(payload)
            "change_sides" -> MqttEvent.SetChangeSides(payload.lowercase() == "on" || payload == "true" || payload == "1")
            "change_sides_anim" -> MqttEvent.SetChangeSidesAnim(payload.lowercase() == "on" || payload == "true" || payload == "1")
            else -> null
        }
        event?.let { _mqttEvents.tryEmit(it) }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            if (client?.isConnected == true) { client?.disconnect() }
            client?.close()
        } catch (e: Exception) { Log.e(TAG, "Error disconnecting") }
    }
}

sealed class MqttEvent {
    object PointLeft : MqttEvent()
    object PointRight : MqttEvent()
    object RemovePoint : MqttEvent()
    data class SetP1Name(val name: String) : MqttEvent()
    data class SetP2Name(val name: String) : MqttEvent()
    data class SetChangeSides(val enabled: Boolean) : MqttEvent()
    data class SetChangeSidesAnim(val enabled: Boolean) : MqttEvent()
}
