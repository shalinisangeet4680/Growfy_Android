package org.mobile.growfy.feature.reviews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mobile.growfy.core.network.ApiResult
import org.mobile.growfy.core.network.DataChanges
import org.mobile.growfy.core.network.DataKind
import org.mobile.growfy.core.network.getOrNull
import org.mobile.growfy.core.network.userMessage
import org.mobile.growfy.core.storage.AppPreferences
import org.mobile.growfy.core.ui.LocationFilter
import org.mobile.growfy.core.ui.Messenger
import org.mobile.growfy.core.ui.UiError
import org.mobile.growfy.core.ui.toUiError
import org.mobile.growfy.domain.model.InboxFilter
import org.mobile.growfy.domain.model.Location
import org.mobile.growfy.domain.model.Review
import org.mobile.growfy.domain.repository.LocationRepository
import org.mobile.growfy.domain.repository.ReviewRepository
import kotlin.time.Clock
import kotlin.time.Instant

data class ReviewsUiState(
    val loading: Boolean = true,
    val error: UiError? = null,
    val refreshing: Boolean = false,
    val query: String = "",
    val filter: InboxFilter = InboxFilter.Pending,
    val locations: List<Location> = emptyList(),
    val selectedLocationId: String? = null,
    val reviews: List<Review> = emptyList(),
    val pending: Int = 0,
    val replied: Int = 0,
    val nextCursor: String? = null,
    val loadingMore: Boolean = false,
    val now: Instant = Clock.System.now(),
)

@OptIn(FlowPreview::class)
class ReviewsViewModel(
    private val reviews: ReviewRepository,
    private val locations: LocationRepository,
    private val filter: LocationFilter,
    private val prefs: AppPreferences,
    private val messenger: Messenger,
    changes: DataChanges,
) : ViewModel() {
    private val _state = MutableStateFlow(ReviewsUiState())
    val state: StateFlow<ReviewsUiState> = _state.asStateFlow()

    val showSwipeHint: StateFlow<Boolean> = prefs.swipeHintSeen.map { !it }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val queries = MutableStateFlow("")
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            merge(
                filter.selectedId.drop(1).map { },
                queries.drop(1).debounce(300).map { },
                changes.of(DataKind.Reviews),
            ).collect { refresh() }
        }
        viewModelScope.launch { locations.locations().getOrNull()?.let { list -> _state.update { it.copy(locations = list) } } }
    }

    fun setQuery(value: String) {
        _state.update { it.copy(query = value) }
        queries.value = value
    }

    fun setFilter(value: InboxFilter) {
        _state.update { it.copy(filter = value, reviews = emptyList(), loading = true) }
        refresh()
    }

    fun selectLocation(id: String?) = filter.select(id)

    fun refresh(pull: Boolean = false) {
        loadJob?.cancel()
        val s = _state.value
        val locationId = filter.selectedId.value
        if (pull) _state.update { it.copy(refreshing = true) }
        loadJob = viewModelScope.launch {
            when (val result = reviews.inbox(s.filter, locationId, s.query)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        error = null,
                        selectedLocationId = locationId,
                        reviews = result.data.reviews,
                        pending = result.data.pending,
                        replied = result.data.replied,
                        nextCursor = result.data.nextCursor,
                        now = Clock.System.now(),
                    )
                }
                is ApiResult.Failure -> {
                    if (pull && s.reviews.isNotEmpty()) messenger.show(result.error.userMessage())
                    _state.update { it.copy(loading = false, refreshing = false, error = result.error.toUiError()) }
                }
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        val cursor = s.nextCursor ?: return
        if (s.loadingMore) return
        _state.update { it.copy(loadingMore = true) }
        viewModelScope.launch {
            val page = reviews.inbox(s.filter, filter.selectedId.value, s.query, cursor).getOrNull()
            _state.update {
                it.copy(
                    loadingMore = false,
                    reviews = if (page == null) it.reviews else (it.reviews + page.reviews).distinctBy { r -> r.id },
                    nextCursor = page?.nextCursor,
                )
            }
        }
    }

    /** Swipe-to-send. Returns false when the send failed so the row can spring back. */
    suspend fun sendDraft(review: Review): Boolean {
        val draft = review.draft ?: return false
        return when (val result = reviews.reply(review.id, draft.text, draft.templateId)) {
            is ApiResult.Success -> {
                messenger.show("Reply sent to ${review.authorFirstName}", "Undo") { reviews.deleteReply(review.id) }
                true
            }
            is ApiResult.Failure -> {
                messenger.show(result.error.userMessage())
                false
            }
        }
    }

    fun dismissSwipeHint() {
        viewModelScope.launch { prefs.setSwipeHintSeen() }
    }
}
