package org.mobile.growfy.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mobile.growfy.core.network.ApiResult
import org.mobile.growfy.core.network.userMessage
import org.mobile.growfy.domain.repository.AuthRepository
import org.mobile.growfy.navigation.Route
import org.mobile.growfy.navigation.SessionRouter

data class LoginUiState(
    val phone: String = "",
    val code: String = "",
    val codeSentTo: String? = null,
    /** The number the code was sent to; verify always uses this, never the editable field. */
    val sentPhone: String? = null,
    val resendIn: Int = 0,
    val loading: Boolean = false,
    val error: String? = null,
    val destination: Route? = null,
) {
    val canSendCode: Boolean get() = phone.count(Char::isDigit) >= 10 && !loading
    val canVerify: Boolean get() = code.length == 6 && !loading
}

class LoginViewModel(private val auth: AuthRepository, private val router: SessionRouter) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()
    private var countdown: Job? = null

    fun setPhone(value: String) = _state.update {
        if (it.codeSentTo != null) it else it.copy(phone = value.filter { c -> c.isDigit() || c == '+' }.take(16), error = null)
    }

    fun setCode(value: String) {
        _state.update { it.copy(code = value.filter(Char::isDigit).take(6), error = null) }
        if (_state.value.code.length == 6) verify()
    }

    fun sendCode() {
        if (_state.value.loading) return
        val phone = _state.value.sentPhone ?: _state.value.phone
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = auth.requestCode(phone)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(loading = false, codeSentTo = result.data.displayPhone, sentPhone = phone, code = "") }
                    startCountdown(result.data.resendInSeconds)
                }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.error.userMessage()) }
            }
        }
    }

    fun verify() {
        val s = _state.value
        val phone = s.sentPhone ?: return
        if (!s.canVerify) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = auth.verifyCode(phone, s.code)) {
                is ApiResult.Success -> _state.update { it.copy(destination = router.destination()) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, code = "", error = result.error.userMessage()) }
            }
        }
    }

    fun changeNumber() {
        countdown?.cancel()
        _state.update { it.copy(codeSentTo = null, sentPhone = null, code = "", error = null, resendIn = 0) }
    }

    private fun startCountdown(seconds: Int) {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            for (left in seconds downTo 0) {
                _state.update { it.copy(resendIn = left) }
                delay(1_000)
            }
        }
    }
}
