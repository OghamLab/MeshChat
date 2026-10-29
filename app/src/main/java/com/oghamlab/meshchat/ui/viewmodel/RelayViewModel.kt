
package com.oghamlab.meshchat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oghamlab.meshchat.nostr.identity.relay.RelayPool
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.flow.StateFlow

class RelayViewModel : ViewModel() {

    private val pool = RelayPool(viewModelScope)

    val relays = pool.relays
    val eventCount = pool.eventCount

    fun addRelay(url: String): Boolean {
        return try {
            pool.addRelay(url)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun removeRelay(url: String) {
        pool.removeRelay(url)
    }

    fun subscribePublicEvents() {
        pool.subscribePublicEvents()
    }

    fun disconnectAll() {
        pool.disconnectAll()
    }

    fun reconnectAll() {
        pool.reconnectAll()
    }

    override fun onCleared() {
        pool.close()
        super.onCleared()
    }
}
