package org.mobile.growfy.feature.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.http.Url
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mobile.growfy.core.config.MobileContract
import org.mobile.growfy.core.network.ApiResult
import org.mobile.growfy.core.network.DataChanges
import org.mobile.growfy.core.network.DataKind
import org.mobile.growfy.core.network.getOrNull
import org.mobile.growfy.core.network.userMessage
import org.mobile.growfy.core.platform.DeepLinks
import org.mobile.growfy.core.platform.UrlLauncher
import org.mobile.growfy.core.ui.Messenger
import org.mobile.growfy.core.ui.UiError
import org.mobile.growfy.core.ui.toUiError
import org.mobile.growfy.domain.model.GoogleConnection
import org.mobile.growfy.domain.model.Location
import org.mobile.growfy.domain.model.Me
import org.mobile.growfy.domain.model.NotificationPrefs
import org.mobile.growfy.domain.model.ReplyMode
import org.mobile.growfy.domain.model.Template
import org.mobile.growfy.domain.repository.AuthRepository
import org.mobile.growfy.domain.repository.GoogleAccountRepository
import org.mobile.growfy.domain.repository.InsightsRepository
import org.mobile.growfy.domain.repository.LocationRepository
import org.mobile.growfy.domain.repository.NotificationRepository
import org.mobile.growfy.domain.repository.TemplateRepository

data class MoreUiState(
    val me: Me? = null,
    val googleEmail: String? = null,
    val locations: List<Location> = emptyList(),
    val templates: List<Template> = emptyList(),
    val connections: Int = 0,
    val notifications: NotificationPrefs? = null,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: UiError? = null,
    val signedOut: Boolean = false,
) {
    val hasData: Boolean get() = me != null || locations.isNotEmpty()
}

class MoreViewModel(
    private val auth: AuthRepository,
    private val locations: LocationRepository,
    private val templates: TemplateRepository,
    private val google: GoogleAccountRepository,
    private val notifications: NotificationRepository,
    private val messenger: Messenger,
    changes: DataChanges,
) : ViewModel() {
    private val _state = MutableStateFlow(MoreUiState())
    val state: StateFlow<MoreUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { changes.of(DataKind.Locations, DataKind.Templates, DataKind.Connections, DataKind.Reviews).collect { refresh() } }
    }

    fun refresh(pull: Boolean = false) {
        if (pull) _state.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            val me = async { auth.me() }
            val locs = async { locations.locations() }
            val tpls = async { templates.templates() }
            val conns = async { google.connections() }
            val prefs = async { notifications.prefs() }
            val locResult = locs.await()
            val connections = conns.await().getOrNull()
            val failure = (locResult as? ApiResult.Failure)?.error
            if (pull && failure != null && _state.value.hasData) messenger.show(failure.userMessage())
            _state.update {
                it.copy(
                    loading = false,
                    refreshing = false,
                    me = me.await().getOrNull() ?: it.me,
                    locations = locResult.getOrNull() ?: it.locations,
                    templates = tpls.await().getOrNull() ?: it.templates,
                    connections = connections?.size ?: it.connections,
                    googleEmail = connections?.firstOrNull()?.email ?: it.googleEmail,
                    notifications = prefs.await().getOrNull() ?: it.notifications,
                    error = failure?.toUiError(),
                )
            }
        }
    }

    fun setReplyMode(location: Location, mode: ReplyMode) {
        _state.update { s -> s.copy(locations = s.locations.map { if (it.id == location.id) it.copy(mode = mode) else it }) }
        viewModelScope.launch {
            when (val result = locations.setReplyMode(location.id, mode)) {
                is ApiResult.Success -> messenger.show("${location.title}: ${if (mode == ReplyMode.Auto) "Auto" else "Approve"}")
                is ApiResult.Failure -> {
                    messenger.show(result.error.userMessage())
                    refresh()
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            auth.signOut()
            _state.update { it.copy(signedOut = true) }
        }
    }
}

data class TemplatesUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: UiError? = null,
    val templates: List<Template> = emptyList(),
)

class TemplatesViewModel(
    private val templates: TemplateRepository,
    private val insights: InsightsRepository,
    private val messenger: Messenger,
) : ViewModel() {
    private val _state = MutableStateFlow(TemplatesUiState())
    val state: StateFlow<TemplatesUiState> = _state.asStateFlow()

    init {
        // Opening this screen is the "Check reply templates" setup step (§3).
        viewModelScope.launch { insights.completeSetupStep("templates") }
        load()
    }

    fun load(pull: Boolean = false) {
        if (pull) _state.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            when (val result = templates.templates()) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, refreshing = false, error = null, templates = result.data) }
                is ApiResult.Failure -> {
                    if (pull && _state.value.templates.isNotEmpty()) messenger.show(result.error.userMessage())
                    _state.update { it.copy(loading = false, refreshing = false, error = result.error.toUiError()) }
                }
            }
        }
    }

    fun setEnabled(template: Template, enabled: Boolean) {
        _state.update { s -> s.copy(templates = s.templates.map { if (it.id == template.id) it.copy(hidden = !enabled) else it }) }
        viewModelScope.launch {
            val result = templates.setHidden(template.id, hidden = !enabled)
            if (result is ApiResult.Failure) {
                messenger.show(result.error.userMessage())
                _state.update { s -> s.copy(templates = s.templates.map { if (it.id == template.id) template else it }) }
            }
        }
    }
}

data class AccountsUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: UiError? = null,
    val connections: List<GoogleConnection> = emptyList(),
    val connecting: Boolean = false,
)

class GoogleAccountsViewModel(
    private val google: GoogleAccountRepository,
    private val locations: LocationRepository,
    private val urls: UrlLauncher,
    private val deepLinks: DeepLinks,
    private val messenger: Messenger,
) : ViewModel() {
    private val _state = MutableStateFlow(AccountsUiState())
    val state: StateFlow<AccountsUiState> = _state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            deepLinks.links.collect { link ->
                if (!link.startsWith(MobileContract.OAUTH_REDIRECT_URI)) return@collect
                deepLinks.consumed()
                val error = Url(link).parameters["error"]
                if (error != null) {
                    _state.update { it.copy(connecting = false) }
                    messenger.show(error)
                } else {
                    onConnected()
                }
            }
        }
    }

    fun refresh(pull: Boolean = false) {
        if (pull) _state.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            when (val result = google.connections()) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, refreshing = false, error = null, connections = result.data) }
                is ApiResult.Failure -> {
                    if (pull && _state.value.connections.isNotEmpty()) messenger.show(result.error.userMessage())
                    _state.update { it.copy(loading = false, refreshing = false, error = result.error.toUiError()) }
                }
            }
        }
    }

    fun addAccount() {
        _state.update { it.copy(connecting = true) }
        viewModelScope.launch {
            when (val result = google.connectUrl()) {
                is ApiResult.Success -> urls.open(result.data)
                is ApiResult.Failure -> {
                    _state.update { it.copy(connecting = false) }
                    messenger.show(result.error.userMessage())
                }
            }
        }
    }

    fun onResume() {
        viewModelScope.launch { onConnected() }
    }

    /** Runs once per connect attempt: the deep link and the screen resuming both land here. */
    private suspend fun onConnected() {
        if (!_state.value.connecting) return
        _state.update { it.copy(connecting = false) }
        val found = locations.discover().getOrNull()
        refresh()
        found?.let { messenger.show(if (it.isEmpty()) "No new locations found" else "${it.size} locations on your accounts") }
    }

    fun disconnect(connection: GoogleConnection) {
        viewModelScope.launch {
            when (val result = google.disconnect(connection.id)) {
                is ApiResult.Success -> {
                    messenger.show("Disconnected ${connection.email}")
                    refresh()
                }
                is ApiResult.Failure -> messenger.show(result.error.userMessage())
            }
        }
    }
}

data class NotificationsUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: UiError? = null,
    val prefs: NotificationPrefs? = null,
)

class NotificationsViewModel(private val repo: NotificationRepository, private val messenger: Messenger) : ViewModel() {
    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load(pull: Boolean = false) {
        if (pull) _state.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            when (val result = repo.prefs()) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, refreshing = false, error = null, prefs = result.data) }
                is ApiResult.Failure -> {
                    if (pull && _state.value.prefs != null) messenger.show(result.error.userMessage())
                    _state.update { it.copy(loading = false, refreshing = false, error = result.error.toUiError()) }
                }
            }
        }
    }

    fun update(transform: (NotificationPrefs) -> NotificationPrefs) {
        val before = _state.value.prefs ?: return
        val after = transform(before)
        _state.update { it.copy(prefs = after) }
        viewModelScope.launch {
            val result = repo.update(after)
            if (result is ApiResult.Failure) {
                messenger.show(result.error.userMessage())
                _state.update { it.copy(prefs = before) }
            }
        }
    }
}
