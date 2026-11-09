package org.mobile.growfy.data.repository

import org.mobile.growfy.core.config.MobileContract
import org.mobile.growfy.core.network.ApiResult
import org.mobile.growfy.core.network.DataChanges
import org.mobile.growfy.core.network.DataKind
import org.mobile.growfy.core.network.onSuccess
import org.mobile.growfy.core.network.safeApiCall
import org.mobile.growfy.data.remote.BusinessApi
import org.mobile.growfy.data.remote.ProposedBusinessApi
import org.mobile.growfy.data.remote.dto.NotificationPrefsDto
import org.mobile.growfy.data.remote.dto.ReplyRequest
import org.mobile.growfy.data.remote.dto.SavePostRequest
import org.mobile.growfy.data.remote.dto.StartOAuthRequest
import org.mobile.growfy.data.remote.dto.UpdateLocationRequest
import org.mobile.growfy.domain.model.DiscoveredLocation
import org.mobile.growfy.domain.model.GoogleConnection
import org.mobile.growfy.domain.model.HomeSummary
import org.mobile.growfy.domain.model.InboxFilter
import org.mobile.growfy.domain.model.InboxPage
import org.mobile.growfy.domain.model.Location
import org.mobile.growfy.domain.model.NotificationPrefs
import org.mobile.growfy.domain.model.Post
import org.mobile.growfy.domain.model.PostContent
import org.mobile.growfy.domain.model.PublishMode
import org.mobile.growfy.domain.model.ReplyMode
import org.mobile.growfy.domain.model.ReplyTone
import org.mobile.growfy.domain.model.Review
import org.mobile.growfy.domain.model.SetupStep
import org.mobile.growfy.domain.model.Stat
import org.mobile.growfy.domain.model.Template
import org.mobile.growfy.domain.repository.GoogleAccountRepository
import org.mobile.growfy.domain.repository.InsightsRepository
import org.mobile.growfy.domain.repository.LocationRepository
import org.mobile.growfy.domain.repository.NotificationRepository
import org.mobile.growfy.domain.repository.PostRepository
import org.mobile.growfy.domain.repository.ReviewRepository
import org.mobile.growfy.domain.repository.TemplateRepository
import kotlin.math.roundToInt
import kotlin.time.Instant

class GoogleAccountRepositoryImpl(private val api: BusinessApi, private val changes: DataChanges) : GoogleAccountRepository {
    override suspend fun connections(): ApiResult<List<GoogleConnection>> =
        safeApiCall { api.connections().data.map { it.toDomain() } }

    override suspend fun connectUrl(): ApiResult<String> =
        safeApiCall { api.startOAuth(StartOAuthRequest(redirectUri = MobileContract.OAUTH_REDIRECT_URI)).url }

    override suspend fun disconnect(connectionId: String): ApiResult<Unit> =
        safeApiCall { api.disconnect(connectionId) }.onSuccess { changes.notify(DataKind.Connections, DataKind.Locations) }
}

class LocationRepositoryImpl(private val api: BusinessApi, private val changes: DataChanges) : LocationRepository {
    override suspend fun locations(): ApiResult<List<Location>> = safeApiCall { api.locations().data.map { it.toDomain() } }

    override suspend fun discover(): ApiResult<List<DiscoveredLocation>> = safeApiCall {
        val result = api.discover()
        (result.added + result.restored + result.already).distinctBy { it.id }.map { DiscoveredLocation(it.id, it.title) }
    }.onSuccess { changes.notify(DataKind.Locations) }

    override suspend fun remove(locationId: String): ApiResult<Unit> =
        safeApiCall { api.removeLocation(locationId) }.onSuccess { changes.notify(DataKind.Locations) }

    /** The backend stores a mode per star rating; the app sets all five at once and keeps each rule's template. */
    override suspend fun setReplyMode(locationId: String, mode: ReplyMode): ApiResult<Unit> = safeApiCall {
        val rules = api.location(locationId).settings.ratingRules
        api.updateLocation(locationId, UpdateLocationRequest(rules.map { it.copy(mode = mode.api) }))
        Unit
    }.onSuccess { changes.notify(DataKind.Locations) }

    override suspend fun website(locationId: String): ApiResult<String?> = safeApiCall { api.location(locationId).info.website }
}

class ReviewRepositoryImpl(private val api: BusinessApi, private val changes: DataChanges) : ReviewRepository {
    override suspend fun inbox(filter: InboxFilter, locationId: String?, query: String?, cursor: String?): ApiResult<InboxPage> =
        safeApiCall {
            val dto = api.inbox(filter.api, locationId, query, cursor)
            InboxPage(dto.data.map { it.toDomain() }, dto.counts.pending, dto.counts.replied, dto.nextCursor)
        }

    override suspend fun review(reviewId: String): ApiResult<Review> = safeApiCall { api.review(reviewId).toDomain() }

    // replyNow on the backend upserts the review's draft and sends it, so this also covers approving a draft.
    override suspend fun reply(reviewId: String, text: String, templateId: String?): ApiResult<Review> =
        safeApiCall { api.reply(reviewId, ReplyRequest(text, templateId)).toDomain() }.notifying()

    override suspend fun deleteReply(reviewId: String): ApiResult<Review> =
        safeApiCall { api.deleteReply(reviewId).toDomain() }.notifying()

    override suspend fun rewrite(reviewId: String, tone: ReplyTone?): ApiResult<Review> =
        safeApiCall { api.aiDraft(reviewId, tone?.api ?: "default").toDomain() }

    override suspend fun skip(reviewId: String): ApiResult<Review> =
        safeApiCall { api.skip(reviewId).toDomain() }.notifying()

    private fun <T> ApiResult<T>.notifying() = onSuccess { changes.notify(DataKind.Reviews, DataKind.Locations) }
}

class PostRepositoryImpl(
    private val api: BusinessApi,
    private val proposed: ProposedBusinessApi,
    private val changes: DataChanges,
) : PostRepository {
    override suspend fun posts(locationId: String?): ApiResult<List<Post>> =
        safeApiCall { api.posts(locationId).data.map { it.toDomain() } }

    override suspend fun create(locationId: String, content: PostContent, mode: PublishMode, scheduledAt: Instant?) =
        safeApiCall {
            api.createPost(SavePostRequest(locationId, content.toDto(), mode.api, scheduledAt?.toString())).toDomain()
        }.onSuccess { changes.notify(DataKind.Posts) }

    override suspend fun update(postId: String, content: PostContent, mode: PublishMode, scheduledAt: Instant?) =
        safeApiCall {
            api.updatePost(postId, SavePostRequest(null, content.toDto(), mode.api, scheduledAt?.toString())).toDomain()
        }.onSuccess { changes.notify(DataKind.Posts) }

    override suspend fun delete(postId: String): ApiResult<Unit> =
        safeApiCall { api.deletePost(postId) }.onSuccess { changes.notify(DataKind.Posts) }

    override suspend fun uploadPhoto(bytes: ByteArray, fileName: String, mimeType: String): ApiResult<String> =
        safeApiCall { proposed.upload(bytes, fileName, mimeType).url }
}

class InsightsRepositoryImpl(private val api: BusinessApi, private val proposed: ProposedBusinessApi) : InsightsRepository {
    override suspend fun home(locationId: String?): ApiResult<HomeSummary> = safeApiCall {
        val dto = proposed.home(locationId)
        HomeSummary(dto.health.score, dto.health.note, dto.setup.steps.map { SetupStep(it.key, it.label, it.done) })
    }

    override suspend fun completeSetupStep(key: String): ApiResult<Unit> = safeApiCall { proposed.completeSetupStep(key) }

    override suspend fun stats(locationId: String?): ApiResult<List<Stat>> = safeApiCall {
        val dto = api.insights(locationId, days = STATS_DAYS)
        val now = dto.current
        val before = dto.previous
        listOf(
            Stat("Profile views", now.impressionsMaps + now.impressionsSearch, change(now.impressionsMaps + now.impressionsSearch, before.impressionsMaps + before.impressionsSearch)),
            Stat("Directions", now.directions, change(now.directions, before.directions)),
            Stat("Website clicks", now.websiteClicks, change(now.websiteClicks, before.websiteClicks)),
            Stat("Calls", now.calls, change(now.calls, before.calls)),
        )
    }

    private fun change(current: Int, previous: Int): Int? =
        if (previous == 0) null else ((current - previous) * 100.0 / previous).roundToInt()

    private companion object {
        const val STATS_DAYS = 30
    }
}

class TemplateRepositoryImpl(private val api: BusinessApi, private val changes: DataChanges) : TemplateRepository {
    override suspend fun templates(): ApiResult<List<Template>> = safeApiCall { api.templates(null).data.map { it.toDomain() } }

    override suspend fun setHidden(templateId: String, hidden: Boolean): ApiResult<Template> =
        safeApiCall { api.setTemplateHidden(templateId, hidden).toDomain() }.onSuccess { changes.notify(DataKind.Templates) }
}

class NotificationRepositoryImpl(private val proposed: ProposedBusinessApi) : NotificationRepository {
    override suspend fun prefs(): ApiResult<NotificationPrefs> =
        safeApiCall { proposed.notificationPrefs().let { NotificationPrefs(it.newReviews, it.lowRatings, it.weeklySummary) } }

    override suspend fun update(prefs: NotificationPrefs): ApiResult<NotificationPrefs> = safeApiCall {
        proposed.updateNotificationPrefs(NotificationPrefsDto(prefs.newReviews, prefs.lowRatings, prefs.weeklySummary))
            .let { NotificationPrefs(it.newReviews, it.lowRatings, it.weeklySummary) }
    }
}
