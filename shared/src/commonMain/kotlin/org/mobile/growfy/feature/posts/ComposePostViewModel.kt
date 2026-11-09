package org.mobile.growfy.feature.posts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.mobile.growfy.core.network.ApiResult
import org.mobile.growfy.core.network.getOrNull
import org.mobile.growfy.core.network.userMessage
import org.mobile.growfy.core.platform.PickedPhoto
import org.mobile.growfy.core.ui.LocationFilter
import org.mobile.growfy.core.ui.Messenger
import org.mobile.growfy.core.util.today
import org.mobile.growfy.domain.model.CtaType
import org.mobile.growfy.domain.model.Location
import org.mobile.growfy.domain.model.Post
import org.mobile.growfy.domain.model.PostContent
import org.mobile.growfy.domain.model.PostCta
import org.mobile.growfy.domain.model.PostOffer
import org.mobile.growfy.domain.model.PostSchedule
import org.mobile.growfy.domain.model.PostStatus
import org.mobile.growfy.domain.model.PostType
import org.mobile.growfy.domain.model.PublishMode
import org.mobile.growfy.domain.repository.LocationRepository
import org.mobile.growfy.domain.repository.PostRepository
import org.mobile.growfy.navigation.Route
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

data class ComposeUiState(
    val loading: Boolean = true,
    val postId: String? = null,
    /** Google doesn't allow editing a published post; it can only be deleted. */
    val readOnly: Boolean = false,
    val locations: List<Location> = emptyList(),
    val locationId: String? = null,
    val type: PostType = PostType.Update,
    val title: String = "",
    val summary: String = "",
    val mediaUrl: String? = null,
    val uploading: Boolean = false,
    val startDate: LocalDate = today(),
    val endDate: LocalDate = today().plus(DatePeriod(days = 7)),
    val couponCode: String = "",
    val scheduled: Boolean = false,
    val scheduledAt: LocalDateTime = today().plus(DatePeriod(days = 1)).atTime(LocalTime(10, 0)),
    val cta: CtaType? = CtaType.LearnMore,
    val ctaUrl: String = "",
    val saving: Boolean = false,
    val closed: Boolean = false,
) {
    val location: Location? get() = locations.firstOrNull { it.id == locationId }

    /** Mirrors backend checkPost so the button is only enabled for a post Google will accept. */
    val problem: String?
        get() = when {
            summary.isBlank() -> "Write the post text."
            summary.length > SUMMARY_MAX -> "Posts can be at most $SUMMARY_MAX characters."
            type.hasSchedule && title.isBlank() -> if (type == PostType.Offer) "Give the offer a title." else "Give the event a title."
            type.hasSchedule && endDate < startDate -> "The end must be after the start."
            cta?.needsUrl == true && !ctaUrl.trim().startsWith("https://") -> "The button needs an https link."
            scheduled && scheduledAt.toInstant(TimeZone.currentSystemDefault()) < Clock.System.now() + 1.minutes ->
                "Schedule the post at least a minute ahead."
            else -> null
        }

    companion object {
        const val SUMMARY_MAX = 1500
    }
}

class ComposePostViewModel(
    private val route: Route.ComposePost,
    private val posts: PostRepository,
    private val locations: LocationRepository,
    private val filter: LocationFilter,
    private val messenger: Messenger,
) : ViewModel() {
    private val _state = MutableStateFlow(ComposeUiState())
    val state: StateFlow<ComposeUiState> = _state.asStateFlow()
    private var original: Post? = null

    init {
        viewModelScope.launch {
            val locs = locations.locations().getOrNull().orEmpty()
            val post = route.postId?.let { id -> posts.posts(null).getOrNull()?.firstOrNull { it.id == id } }
            original = post
            val tz = TimeZone.currentSystemDefault()
            _state.update { s ->
                if (post != null) {
                    val c = post.content
                    s.copy(
                        loading = false,
                        postId = post.id,
                        readOnly = post.status == PostStatus.Published,
                        locations = locs,
                        locationId = post.locationId,
                        type = c.type,
                        title = c.schedule?.title.orEmpty(),
                        summary = c.summary,
                        mediaUrl = c.mediaUrl,
                        startDate = c.schedule?.start?.toLocalDateTime(tz)?.date ?: s.startDate,
                        endDate = c.schedule?.end?.toLocalDateTime(tz)?.date ?: s.endDate,
                        couponCode = c.offer?.couponCode.orEmpty(),
                        scheduled = post.status == PostStatus.Scheduled,
                        scheduledAt = post.scheduledAt?.toLocalDateTime(tz) ?: s.scheduledAt,
                        cta = c.cta?.type,
                        ctaUrl = c.cta?.url.orEmpty(),
                    )
                } else {
                    val preset = route.scheduleAt?.let { Instant.parse(it).toLocalDateTime(tz) }
                    s.copy(
                        loading = false,
                        locations = locs,
                        locationId = filter.selectedId.value?.takeIf { id -> locs.any { it.id == id } } ?: locs.firstOrNull()?.id,
                        scheduled = preset != null,
                        scheduledAt = preset ?: s.scheduledAt,
                    )
                }
            }
            if (post == null) prefillWebsite()
        }
    }

    fun setType(value: PostType) = _state.update { it.copy(type = value) }
    fun setTitle(value: String) = _state.update { it.copy(title = value.take(58)) }
    fun setSummary(value: String) = _state.update { it.copy(summary = value.take(ComposeUiState.SUMMARY_MAX)) }
    fun setCoupon(value: String) = _state.update { it.copy(couponCode = value.take(58)) }
    fun setStart(value: LocalDate) = _state.update { it.copy(startDate = value, endDate = maxOf(value, it.endDate)) }
    fun setEnd(value: LocalDate) = _state.update { it.copy(endDate = value) }
    fun setScheduled(value: Boolean) = _state.update { it.copy(scheduled = value) }
    fun setScheduledAt(value: LocalDateTime) = _state.update { it.copy(scheduledAt = value) }
    fun setCtaUrl(value: String) = _state.update { it.copy(ctaUrl = value.trim()) }
    fun removePhoto() = _state.update { it.copy(mediaUrl = null) }

    fun setLocation(id: String) {
        _state.update { it.copy(locationId = id) }
        prefillWebsite()
    }

    fun setCta(value: CtaType?) {
        _state.update { it.copy(cta = value) }
        if (_state.value.ctaUrl.isBlank()) prefillWebsite()
    }

    /** The button link defaults to the location's website, which is what most owners want. */
    private fun prefillWebsite() {
        val id = _state.value.locationId ?: return
        viewModelScope.launch {
            val site = locations.website(id).getOrNull()
            if (site != null && _state.value.ctaUrl.isBlank()) _state.update { it.copy(ctaUrl = site) }
        }
    }

    fun uploadPhoto(photo: PickedPhoto) {
        if (photo.bytes.size > MAX_PHOTO_BYTES) {
            messenger.show("Google takes photos up to 5 MB.")
            return
        }
        _state.update { it.copy(uploading = true) }
        viewModelScope.launch {
            when (val result = posts.uploadPhoto(photo.bytes, photo.fileName, photo.mimeType)) {
                is ApiResult.Success -> _state.update { it.copy(uploading = false, mediaUrl = result.data) }
                is ApiResult.Failure -> {
                    _state.update { it.copy(uploading = false) }
                    messenger.show(result.error.userMessage())
                }
            }
        }
    }

    fun publish() = save(if (_state.value.scheduled) PublishMode.Schedule else PublishMode.Now)

    fun saveDraft() = save(PublishMode.Draft)

    private fun save(mode: PublishMode) {
        val s = _state.value
        val locationId = s.locationId ?: return
        if (mode != PublishMode.Draft && s.problem != null) return
        _state.update { it.copy(saving = true) }
        val tz = TimeZone.currentSystemDefault()
        val content = PostContent(
            type = s.type,
            summary = s.summary,
            mediaUrl = s.mediaUrl,
            cta = s.cta?.let { PostCta(it, s.ctaUrl.ifBlank { null }) },
            schedule = if (s.type.hasSchedule) {
                PostSchedule(s.title, s.startDate.atTime(LocalTime(0, 0)).toInstant(tz), s.endDate.atTime(LocalTime(23, 59)).toInstant(tz))
            } else null,
            offer = if (s.type == PostType.Offer && s.couponCode.isNotBlank()) PostOffer(s.couponCode, null, null) else null,
        )
        val at = if (mode == PublishMode.Schedule) s.scheduledAt.toInstant(tz) else null
        viewModelScope.launch {
            val result = s.postId?.let { posts.update(it, content, mode, at) } ?: posts.create(locationId, content, mode, at)
            when (result) {
                is ApiResult.Success -> {
                    messenger.show(
                        when (mode) {
                            PublishMode.Now -> "Posted to Google"
                            PublishMode.Schedule -> "Post scheduled"
                            PublishMode.Draft -> "Draft saved"
                        },
                    )
                    _state.update { it.copy(saving = false, closed = true) }
                }
                is ApiResult.Failure -> {
                    _state.update { it.copy(saving = false) }
                    messenger.show(result.error.userMessage())
                }
            }
        }
    }

    // No Undo: the backend can't restore a deleted post, and re-creating one republishes it (docs/backend-gaps.md §8).
    fun delete() {
        val id = _state.value.postId ?: return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            when (val result = posts.delete(id)) {
                is ApiResult.Success -> {
                    messenger.show("Post deleted")
                    _state.update { it.copy(saving = false, closed = true) }
                }
                is ApiResult.Failure -> {
                    _state.update { it.copy(saving = false) }
                    messenger.show(result.error.userMessage())
                }
            }
        }
    }

    private companion object {
        const val MAX_PHOTO_BYTES = 5 * 1024 * 1024
    }
}
