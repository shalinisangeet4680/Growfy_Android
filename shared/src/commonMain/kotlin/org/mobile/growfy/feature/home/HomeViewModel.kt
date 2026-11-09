package org.mobile.growfy.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mobile.growfy.core.network.ApiResult
import org.mobile.growfy.core.network.DataChanges
import org.mobile.growfy.core.network.DataKind
import org.mobile.growfy.core.network.getOrNull
import org.mobile.growfy.core.network.userMessage
import org.mobile.growfy.core.ui.LocationFilter
import org.mobile.growfy.core.ui.Messenger
import org.mobile.growfy.core.ui.UiError
import org.mobile.growfy.core.ui.toUiError
import org.mobile.growfy.domain.model.HomeSummary
import org.mobile.growfy.domain.model.InboxFilter
import org.mobile.growfy.domain.model.Location
import org.mobile.growfy.domain.model.Me
import org.mobile.growfy.domain.model.Review
import org.mobile.growfy.domain.model.Stat
import org.mobile.growfy.domain.repository.AuthRepository
import org.mobile.growfy.domain.repository.InsightsRepository
import org.mobile.growfy.domain.repository.LocationRepository
import org.mobile.growfy.domain.repository.ReviewRepository
import kotlin.time.Clock
import kotlin.time.Instant

data class HomeUiState(
    val loading: Boolean = true,
    val error: UiError? = null,
    val refreshing: Boolean = false,
    val me: Me? = null,
    val locations: List<Location> = emptyList(),
    val selectedLocationId: String? = null,
    val summary: HomeSummary? = null,
    val focus: Review? = null,
    val waiting: Int = 0,
    val stats: List<Stat> = emptyList(),
    val sending: Boolean = false,
    val now: Instant = Clock.System.now(),
) {
    fun locationName(id: String): String? = locations.firstOrNull { it.id == id }?.title

    val hasData: Boolean get() = me != null || locations.isNotEmpty() || stats.isNotEmpty() || focus != null
}

class HomeViewModel(
    private val auth: AuthRepository,
    private val locations: LocationRepository,
    private val reviews: ReviewRepository,
    private val insights: InsightsRepository,
    private val filter: LocationFilter,
    private val messenger: Messenger,
    changes: DataChanges,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            merge(filter.selectedId.drop(1), changes.of(DataKind.Reviews, DataKind.Posts, DataKind.Locations, DataKind.Templates))
                .collect { refresh() }
        }
    }

    fun selectLocation(id: String?) = filter.select(id)

    /** [pull] = the user pulled to refresh: show the indicator and report failures that keep stale data on screen. */
    fun refresh(pull: Boolean = false) {
        val locationId = filter.selectedId.value
        if (pull) _state.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            val me = async { auth.me() }
            val locs = async { locations.locations() }
            val inbox = async { reviews.inbox(InboxFilter.Pending, locationId) }
            val summary = async { insights.home(locationId) }
            val stats = async { insights.stats(locationId) }
            val inboxResult = inbox.await()
            val failure = (inboxResult as? ApiResult.Failure)?.error
            if (pull && failure != null && _state.value.hasData) messenger.show(failure.userMessage())
            _state.update {
                it.copy(
                    loading = false,
                    refreshing = false,
                    error = failure?.toUiError(),
                    me = me.await().getOrNull() ?: it.me,
                    locations = locs.await().getOrNull() ?: it.locations,
                    selectedLocationId = locationId,
                    focus = inboxResult.getOrNull()?.reviews?.firstOrNull(),
                    waiting = inboxResult.getOrNull()?.pending ?: it.waiting,
                    // §3 GET /business/home: until it ships the health and setup cards stay hidden.
                    summary = summary.await().getOrNull(),
                    stats = stats.await().getOrNull() ?: it.stats,
                    now = Clock.System.now(),
                )
            }
        }
    }

    fun sendFocus() {
        val review = _state.value.focus ?: return
        val draft = review.draft ?: return
        _state.update { it.copy(sending = true) }
        viewModelScope.launch {
            when (val result = reviews.reply(review.id, draft.text, draft.templateId)) {
                is ApiResult.Success -> messenger.show("Reply posted", "Undo") { reviews.deleteReply(review.id) }
                is ApiResult.Failure -> messenger.show(result.error.userMessage())
            }
            _state.update { it.copy(sending = false) }
        }
    }
}
