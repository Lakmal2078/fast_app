package com.example.fastcash.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fastcash.BuildConfig
import com.example.fastcash.data.remote.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest

data class SignedInCustomer(val scope: String, val api: FastCashApiService)

class CustomerSessionModel : ViewModel() {
    val customer = MutableStateFlow<SignedInCustomer?>(null)
    val busy = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    fun signIn(key: String) {
        if (busy.value) return
        busy.value = true
        error.value = null
        viewModelScope.launch {
            try {
                val api = NetworkClient.authenticated(key.trim())
                val response = api.session()
                val id = response.body()?.customerId
                if (response.isSuccessful && !id.isNullOrBlank()) {
                    val scope = MessageDigest.getInstance("SHA-256")
                        .digest("${BuildConfig.BACKEND_BASE_URL}|$id".toByteArray())
                        .joinToString("") { "%02x".format(it) }
                    customer.value = SignedInCustomer(scope, api)
                } else {
                    error.value = if (response.code() == 401) "ප්‍රවේශ යතුර පරීක්ෂා කරන්න." else "සේවාව ලබාගත නොහැක."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error.value = "සේවාවට සම්බන්ධ විය නොහැක. නැවත උත්සාහ කරන්න."
            } finally {
                busy.value = false
            }
        }
    }
}
