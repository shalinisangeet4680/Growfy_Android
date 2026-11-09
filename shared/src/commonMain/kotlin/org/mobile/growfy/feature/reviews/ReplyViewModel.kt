package org.mobile.growfy.feature.reviews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mobile.growfy.core.network.ApiResult
import org.mobile.growfy.core.network.getOrNull
import org.mobile.growfy.core.network.userMessage
import org.mobile.growfy.core.ui.LocationFilter
import org.mobile.growfy.core.ui.Messenger
import org.mobile.growfy.core.ui.UiError
import org.mobile.growfy.core.ui.toUiError
import org.mobile.growfy.domain.model.InboxFilter
import org.mobile.growfy.domain.model.ReplyTone
import org.mobile.growfy.domain.model.Review
import org.mobile.growfy.domain.repository.LocationRepository
import org.mobile.growfy.domain.repository.ReviewRepository
import org.mobile.growfy.domain.repository.TemplateRepository
import kotlin.time.Clock
import kotlin.time.Instant

sealed interface ReplyNavigation {
    data class Next(val reviewId: String) : ReplyNavigation
    data object Close : ReplyNavigation
}

data class ReplyUiState(
    val loading: Boolean = true,
    val error: UiError? = null,
    val refreshing: Boolean = false,
    val review: Review? = null,
    val locationName: String? = null,
    val templateName: String? = null,
    val text: String = "",
    val tone: ReplyTone? = null,
    val rewriting: Boolean = false,
    val sending: Boolean = false,
    val navigation: ReplyNavigation? = null,
    val now: Instant = Clock.System.now(),
)

class ReplyViewModel(
    private val reviewId: String,
    private val reviews: ReviewRepository,
    private val locations: LocationRepository,
    private val templates: TemplateRepository,
    private val filter: LocationFilter,
    private val messenger: Messenger,
) : ViewModel() {
    private val _state = MutableStateFlow(ReplyUiState())
    val state: StateFlow<ReplyUiState> = _state.asStateFlow()

    init {
        load()
    }

    /** A pull keeps the reply being typed; only the review itself is reloaded. */
    fun load(pull: Boolean = false) {
        _state.update { if (pull) it.copy(refreshing = true) else it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val locationList = async { locations.locations().getOrNull().orEmpty() }
            val templateList = async { templates.templates().getOrNull().orEmpty() }
            when (val result = reviews.review(reviewId)) {
                is ApiResult.Success -> {
                    val review = result.data
                    _state.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            error = null,
                            review = review,
                            text = if (pull && it.text.isNotBlank()) it.text else review.draft?.text.orEmpty(),
                            locationName = locationList.await().firstOrNull { l -> l.id == review.locationId }?.title,
                            templateName = review.draft?.templateId?.let { id -> templateList.await().firstOrNull { t -> t.id == id }?.name },
                        )
                    }
                }
                is ApiResult.Failure -> {
                    if (pull && _state.value.review != null) messenger.show(result.error.userMessage())
                    _state.update { it.copy(loading = false, refreshing = false, error = result.error.toUiError()) }
                }
            }
        }
    }

    fun setText(value: String) = _state.update { it.copy(text = value.take(REPLY_MAX)) }

    /** Tapping the selected tone again goes back to the default draft. */
    fun pickTone(tone: ReplyTone) {
        val next = if (_state.value.tone == tone) null else tone
        _state.update { it.copy(tone = next, rewriting = true) }
        viewModelScope.launch {
            when (val result = reviews.rewrite(reviewId, next)) {
                is ApiResult.Success -> _state.update {
                    it.copy(rewriting = false, text = result.data.draft?.text ?: it.text, templateName = null)
                }
                is ApiResult.Failure -> {
                    _state.update { it.copy(rewriting = false) }
                    messenger.show(result.error.userMessage())
                }
            }
        }
    }

    fun send() {
        val s = _state.value
        val review = s.review ?: return
        if (s.text.isBlank()) return
        _state.update { it.copy(sending = true) }
        viewModelScope.launch {
            when (val result = reviews.reply(review.id, s.text.trim(), review.draft?.templateId.takeIf { s.tone == null })) {
                is ApiResult.Success -> {
                    messenger.show("Reply posted", "Undo") { reviews.deleteReply(review.id) }
                    _state.update { it.copy(sending = false, navigation = nextWaiting()?.let(ReplyNavigation::Next) ?: ReplyNavigation.Close) }
                }
                is ApiResult.Failure -> {
                    _state.update { it.copy(sending = false) }
                    messenger.show(result.error.userMessage())
                }
            }
        }
    }

    /** Skip marks the review as skipped on the backend, then moves to the next waiting one. */
    fun skip() {
        viewModelScope.launch {
            reviews.skip(reviewId)
            _state.update { it.copy(navigation = nextWaiting()?.let(ReplyNavigation::Next) ?: ReplyNavigation.Close) }
        }
    }

    fun navigationHandled() = _state.update { it.copy(navigation = null) }

    private suspend fun nextWaiting(): String? =
        reviews.inbox(InboxFilter.Pending, filter.selectedId.value).getOrNull()?.reviews?.firstOrNull { it.id != reviewId }?.id

    private companion object {
        const val REPLY_MAX = 4096
    }
}
