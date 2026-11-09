package org.mobile.growfy.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import org.mobile.growfy.data.remote.dto.AiDraftRequest
import org.mobile.growfy.data.remote.dto.BusinessAnalyticsDto
import org.mobile.growfy.data.remote.dto.BusinessInboxDto
import org.mobile.growfy.data.remote.dto.BusinessInsightsDto
import org.mobile.growfy.data.remote.dto.ConnectUrlDto
import org.mobile.growfy.data.remote.dto.GbpConnectionDto
import org.mobile.growfy.data.remote.dto.GbpLocationDetailDto
import org.mobile.growfy.data.remote.dto.GbpLocationDto
import org.mobile.growfy.data.remote.dto.GbpPostDto
import org.mobile.growfy.data.remote.dto.GbpReviewDto
import org.mobile.growfy.data.remote.dto.GbpTemplateDto
import org.mobile.growfy.data.remote.dto.HomeSummaryDto
import org.mobile.growfy.data.remote.dto.ListDto
import org.mobile.growfy.data.remote.dto.LocationDiscoveryDto
import org.mobile.growfy.data.remote.dto.NotificationPrefsDto
import org.mobile.growfy.data.remote.dto.ReplyRequest
import org.mobile.growfy.data.remote.dto.SavePostRequest
import org.mobile.growfy.data.remote.dto.StartOAuthRequest
import org.mobile.growfy.data.remote.dto.UpdateLocationRequest
import org.mobile.growfy.data.remote.dto.UploadDto
import org.mobile.growfy.data.remote.dto.VisibilityRequest

/** `/api/v1/business` endpoints. Endpoints and params marked with a § are new; see docs/backend-gaps.md. */
class BusinessApi(private val client: HttpClient) {

    suspend fun connections(): ListDto<GbpConnectionDto> = client.get("business/connections").body()

    suspend fun startOAuth(body: StartOAuthRequest): ConnectUrlDto =
        client.post("business/oauth/start") { setBody(body) }.body()

    suspend fun disconnect(connectionId: String) {
        client.delete("business/connections/$connectionId")
    }

    suspend fun discover(): LocationDiscoveryDto = client.post("business/discover").body()

    suspend fun locations(): ListDto<GbpLocationDto> = client.get("business/locations").body()

    suspend fun location(id: String): GbpLocationDetailDto = client.get("business/locations/$id").body()

    suspend fun updateLocation(id: String, body: UpdateLocationRequest): GbpLocationDetailDto =
        client.put("business/locations/$id") { setBody(body) }.body()

    suspend fun removeLocation(id: String) {
        client.delete("business/locations/$id")
    }

    /**
     * [filter]: pending | replied | all. [query] and [cursor] are new (§5) and only sent when used,
     * because the backend rejects unknown query params.
     */
    suspend fun inbox(filter: String, locationId: String?, query: String? = null, cursor: String? = null): BusinessInboxDto =
        client.get("business/inbox") {
            parameter("filter", filter)
            parameter("locationId", locationId)
            parameter("q", query?.takeIf { it.isNotBlank() })
            parameter("cursor", cursor)
        }.body()

    /** §7: new. */
    suspend fun review(reviewId: String): GbpReviewDto = client.get("business/reviews/$reviewId").body()

    suspend fun reply(reviewId: String, body: ReplyRequest): GbpReviewDto =
        client.post("business/reviews/$reviewId/reply") { setBody(body) }.body()

    suspend fun deleteReply(reviewId: String): GbpReviewDto = client.delete("business/reviews/$reviewId/reply").body()

    suspend fun aiDraft(reviewId: String, tone: String): GbpReviewDto =
        client.post("business/reviews/$reviewId/ai-draft") { setBody(AiDraftRequest(tone = tone)) }.body()

    suspend fun skip(reviewId: String): GbpReviewDto = client.post("business/reviews/$reviewId/skip").body()

    suspend fun analytics(locationId: String?, days: Int): BusinessAnalyticsDto =
        client.get("business/analytics") {
            parameter("locationId", locationId)
            parameter("days", days)
        }.body()

    suspend fun insights(locationId: String?, days: Int): BusinessInsightsDto =
        client.get("business/insights") {
            parameter("locationId", locationId)
            parameter("days", days)
        }.body()

    suspend fun templates(locationId: String?): ListDto<GbpTemplateDto> =
        client.get("business/templates") { parameter("locationId", locationId) }.body()

    suspend fun setTemplateHidden(id: String, hidden: Boolean): GbpTemplateDto =
        client.put("business/templates/$id/visibility") { setBody(VisibilityRequest(hidden)) }.body()

    suspend fun posts(locationId: String?): ListDto<GbpPostDto> =
        client.get("business/posts") { parameter("locationId", locationId) }.body()

    suspend fun createPost(body: SavePostRequest): GbpPostDto = client.post("business/posts") { setBody(body) }.body()

    suspend fun updatePost(id: String, body: SavePostRequest): GbpPostDto =
        client.put("business/posts/$id") { setBody(body.copy(locationId = null)) }.body()

    suspend fun deletePost(id: String) {
        client.delete("business/posts/$id")
    }
}

/** Endpoints that are entirely new on the backend. See docs/backend-gaps.md. */
class ProposedBusinessApi(private val client: HttpClient) {

    /** §3 */
    suspend fun home(locationId: String?): HomeSummaryDto =
        client.get("business/home") { parameter("locationId", locationId) }.body()

    /** §3 */
    suspend fun completeSetupStep(key: String) {
        client.post("business/setup/steps/$key/complete")
    }

    /** §6 */
    suspend fun upload(bytes: ByteArray, fileName: String, mimeType: String): UploadDto =
        client.post("business/uploads") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            "file",
                            bytes,
                            Headers.build {
                                append(HttpHeaders.ContentType, mimeType)
                                append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                            },
                        )
                    },
                ),
            )
        }.body()

    /** §4 */
    suspend fun notificationPrefs(): NotificationPrefsDto = client.get("notifications/preferences").body()

    /** §4 */
    suspend fun updateNotificationPrefs(prefs: NotificationPrefsDto): NotificationPrefsDto =
        client.put("notifications/preferences") { setBody(prefs) }.body()
}
