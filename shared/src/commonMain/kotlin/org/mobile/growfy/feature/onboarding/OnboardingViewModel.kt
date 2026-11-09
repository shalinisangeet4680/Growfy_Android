package org.mobile.growfy.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.http.Url
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mobile.growfy.core.config.MobileContract
import org.mobile.growfy.core.network.ApiResult
import org.mobile.growfy.core.network.userMessage
import org.mobile.growfy.core.platform.DeepLinks
import org.mobile.growfy.core.platform.UrlLauncher
import org.mobile.growfy.core.ui.Messenger
import org.mobile.growfy.domain.model.DiscoveredLocation
import org.mobile.growfy.domain.model.ReplyMode
import org.mobile.growfy.domain.repository.GoogleAccountRepository
import org.mobile.growfy.domain.repository.LocationRepository

enum class OnboardingStep { Connect, PickLocations, ReplyMode }

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.Connect,
    val connecting: Boolean = false,
    val locations: List<DiscoveredLocation> = emptyList(),
    val categories: Map<String, String?> = emptyMap(),
    val selected: Set<String> = emptySet(),
    val mode: ReplyMode = ReplyMode.Approval,
    val finishing: Boolean = false,
    val error: String? = null,
    val done: Boolean = false,
)

class OnboardingViewModel(
    private val google: GoogleAccountRepository,
    private val locations: LocationRepository,
    private val urls: UrlLauncher,
    private val deepLinks: DeepLinks,
    private val messenger: Messenger,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            deepLinks.links.collect { link ->
                if (!link.startsWith(MobileContract.OAUTH_REDIRECT_URI)) return@collect
                deepLinks.consumed()
                val error = Url(link).parameters["error"]
                if (error != null) _state.update { it.copy(connecting = false, error = error) } else discover()
            }
        }
    }

    fun connect() {
        _state.update { it.copy(connecting = true, error = null) }
        viewModelScope.launch {
            when (val result = google.connectUrl()) {
                is ApiResult.Success -> urls.open(result.data)
                is ApiResult.Failure -> _state.update { it.copy(connecting = false, error = result.error.userMessage()) }
            }
        }
    }

    /** Back from the browser without a deep link (e.g. the user closed the tab): check whether it worked anyway. */
    fun onResume() {
        if (!_state.value.connecting || _state.value.step != OnboardingStep.Connect) return
        viewModelScope.launch {
            val connected = (google.connections() as? ApiResult.Success)?.data?.isNotEmpty() == true
            if (connected) discover() else _state.update { it.copy(connecting = false) }
        }
    }

    private var discovering = false

    /** The deep link and the screen resuming can both trigger this; only the first one runs. */
    private suspend fun discover() {
        if (discovering || _state.value.step != OnboardingStep.Connect) return
        discovering = true
        _state.update { it.copy(connecting = true, error = null) }
        when (val result = locations.discover()) {
            is ApiResult.Success -> {
                val categories = (locations.locations() as? ApiResult.Success)?.data.orEmpty().associate { it.id to it.category }
                _state.update {
                    it.copy(
                        step = OnboardingStep.PickLocations,
                        connecting = false,
                        locations = result.data,
                        categories = categories,
                        selected = result.data.map { l -> l.id }.toSet(),
                    )
                }
            }
            is ApiResult.Failure -> _state.update { it.copy(connecting = false, error = result.error.userMessage()) }
        }
        discovering = false
    }

    fun toggle(id: String) = _state.update {
        it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id)
    }

    fun next() = _state.update { if (it.selected.isEmpty()) it else it.copy(step = OnboardingStep.ReplyMode) }

    fun back() = _state.update {
        it.copy(step = OnboardingStep.entries[(it.step.ordinal - 1).coerceAtLeast(0)], error = null)
    }

    fun pickMode(mode: ReplyMode) = _state.update { it.copy(mode = mode) }

    fun finish() {
        val s = _state.value
        _state.update { it.copy(finishing = true, error = null) }
        viewModelScope.launch {
            // Discovery imports every location; drop the ones the owner unticked, then apply the reply mode.
            val failure = s.locations.firstNotNullOfOrNull { location ->
                val result = if (location.id in s.selected) locations.setReplyMode(location.id, s.mode) else locations.remove(location.id)
                (result as? ApiResult.Failure)?.error
            }
            if (failure != null) {
                _state.update { it.copy(finishing = false, error = failure.userMessage()) }
            } else {
                messenger.show("You’re all set")
                _state.update { it.copy(finishing = false, done = true) }
            }
        }
    }
}
