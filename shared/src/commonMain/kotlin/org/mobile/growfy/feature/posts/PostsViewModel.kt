package org.mobile.growfy.feature.posts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.mobile.growfy.core.network.ApiResult
import org.mobile.growfy.core.network.DataChanges
import org.mobile.growfy.core.network.DataKind
import org.mobile.growfy.core.network.getOrNull
import org.mobile.growfy.core.network.userMessage
import org.mobile.growfy.core.ui.LocationFilter
import org.mobile.growfy.core.ui.Messenger
import org.mobile.growfy.core.ui.UiError
import org.mobile.growfy.core.ui.toUiError
import org.mobile.growfy.core.util.today
import org.mobile.growfy.domain.model.Location
import org.mobile.growfy.domain.model.Post
import org.mobile.growfy.domain.model.PostStatus
import org.mobile.growfy.domain.repository.LocationRepository
import org.mobile.growfy.domain.repository.PostRepository

enum class PostFilter(val label: String, val status: PostStatus?) {
    All("All", null), Scheduled("Scheduled", PostStatus.Scheduled), Live("Live", PostStatus.Published), Drafts("Drafts", PostStatus.Draft),
}

data class PostsUiState(
    val loading: Boolean = true,
    val error: UiError? = null,
    val refreshing: Boolean = false,
    val today: LocalDate = today(),
    val stripStart: LocalDate = today(),
    val selectedDay: LocalDate = today(),
    /** True when the list is narrowed to [selectedDay]. */
    val dayFiltered: Boolean = false,
    val filter: PostFilter = PostFilter.All,
    val posts: List<Post> = emptyList(),
    val locations: List<Location> = emptyList(),
) {
    val days: List<LocalDate> get() = (0 until 7).map { stripStart.plus(DatePeriod(days = it)) }

    fun postsOn(day: LocalDate) = posts.filter { it.localDate == day }

    val visible: List<Post>
        get() = posts
            .filter { filter.status == null || it.status == filter.status }
            .filter { !dayFiltered || it.localDate == selectedDay }
            .sortedByDescending { it.date }

    fun locationName(id: String) = locations.firstOrNull { it.id == id }?.title
}

val Post.localDate: LocalDate get() = date.toLocalDateTime(TimeZone.currentSystemDefault()).date

class PostsViewModel(
    private val posts: PostRepository,
    private val locations: LocationRepository,
    private val filter: LocationFilter,
    private val messenger: Messenger,
    changes: DataChanges,
) : ViewModel() {
    private val _state = MutableStateFlow(PostsUiState())
    val state: StateFlow<PostsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            merge(filter.selectedId.drop(1).map { }, changes.of(DataKind.Posts)).collect { refresh() }
        }
    }

    fun refresh(pull: Boolean = false) {
        if (pull) _state.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            val locs = locations.locations().getOrNull()
            when (val result = posts.posts(filter.selectedId.value)) {
                is ApiResult.Success -> _state.update {
                    it.copy(loading = false, refreshing = false, error = null, posts = result.data, locations = locs ?: it.locations, today = today())
                }
                is ApiResult.Failure -> {
                    if (pull && _state.value.posts.isNotEmpty()) messenger.show(result.error.userMessage())
                    _state.update { it.copy(loading = false, refreshing = false, error = result.error.toUiError()) }
                }
            }
        }
    }

    /** Same rules as the prototype: tap a day to filter to it; tap the selected day again to show everything. */
    fun pickDay(day: LocalDate) = _state.update {
        if (it.dayFiltered && it.selectedDay == day) it.copy(dayFiltered = false, selectedDay = it.today)
        else it.copy(selectedDay = day, dayFiltered = day != it.today)
    }

    fun jumpTo(day: LocalDate) = _state.update { it.copy(stripStart = day, selectedDay = day, dayFiltered = true) }

    fun clearDay() = _state.update { it.copy(dayFiltered = false, selectedDay = it.today, stripStart = it.today) }

    fun setFilter(value: PostFilter) = _state.update { it.copy(filter = value) }
}
