package org.mobile.growfy.domain.repository

import kotlinx.coroutines.flow.Flow
import org.mobile.growfy.core.network.ApiResult
import org.mobile.growfy.domain.model.DiscoveredLocation
import org.mobile.growfy.domain.model.GoogleConnection
import org.mobile.growfy.domain.model.HomeSummary
import org.mobile.growfy.domain.model.InboxFilter
import org.mobile.growfy.domain.model.InboxPage
import org.mobile.growfy.domain.model.Location
import org.mobile.growfy.domain.model.Me
import org.mobile.growfy.domain.model.NotificationPrefs
import org.mobile.growfy.domain.model.OtpChallenge
import org.mobile.growfy.domain.model.Post
import org.mobile.growfy.domain.model.PostContent
import org.mobile.growfy.domain.model.PublishMode
import org.mobile.growfy.domain.model.ReplyMode
import org.mobile.growfy.domain.model.ReplyTone
import org.mobile.growfy.domain.model.Review
import org.mobile.growfy.domain.model.Stat
import org.mobile.growfy.domain.model.Template
import kotlin.time.Instant

interface AuthRepository {
    val isSignedIn: Flow<Boolean>
    suspend fun requestCode(phone: String): ApiResult<OtpChallenge>
    suspend fun verifyCode(phone: String, code: String): ApiResult<Unit>
    suspend fun me(): ApiResult<Me>
    suspend fun signOut()
}

interface GoogleAccountRepository {
    suspend fun connections(): ApiResult<List<GoogleConnection>>
    /** URL to open in the browser; Google redirects back to the app's deep link when done. */
    suspend fun connectUrl(): ApiResult<String>
    suspend fun disconnect(connectionId: String): ApiResult<Unit>
}

interface LocationRepository {
    suspend fun locations(): ApiResult<List<Location>>
    suspend fun discover(): ApiResult<List<DiscoveredLocation>>
    suspend fun remove(locationId: String): ApiResult<Unit>
    suspend fun setReplyMode(locationId: String, mode: ReplyMode): ApiResult<Unit>
    suspend fun website(locationId: String): ApiResult<String?>
}

interface ReviewRepository {
    suspend fun inbox(filter: InboxFilter, locationId: String?, query: String? = null, cursor: String? = null): ApiResult<InboxPage>
    suspend fun review(reviewId: String): ApiResult<Review>
    suspend fun reply(reviewId: String, text: String, templateId: String?): ApiResult<Review>
    suspend fun deleteReply(reviewId: String): ApiResult<Review>
    suspend fun rewrite(reviewId: String, tone: ReplyTone?): ApiResult<Review>
    suspend fun skip(reviewId: String): ApiResult<Review>
}

interface PostRepository {
    suspend fun posts(locationId: String?): ApiResult<List<Post>>
    suspend fun create(locationId: String, content: PostContent, mode: PublishMode, scheduledAt: Instant?): ApiResult<Post>
    suspend fun update(postId: String, content: PostContent, mode: PublishMode, scheduledAt: Instant?): ApiResult<Post>
    suspend fun delete(postId: String): ApiResult<Unit>
    suspend fun uploadPhoto(bytes: ByteArray, fileName: String, mimeType: String): ApiResult<String>
}

interface InsightsRepository {
    suspend fun home(locationId: String?): ApiResult<HomeSummary>
    suspend fun completeSetupStep(key: String): ApiResult<Unit>
    suspend fun stats(locationId: String?): ApiResult<List<Stat>>
}

interface TemplateRepository {
    suspend fun templates(): ApiResult<List<Template>>
    suspend fun setHidden(templateId: String, hidden: Boolean): ApiResult<Template>
}

interface NotificationRepository {
    suspend fun prefs(): ApiResult<NotificationPrefs>
    suspend fun update(prefs: NotificationPrefs): ApiResult<NotificationPrefs>
}
