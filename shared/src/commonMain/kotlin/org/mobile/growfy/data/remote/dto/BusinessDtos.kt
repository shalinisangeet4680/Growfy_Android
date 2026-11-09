package org.mobile.growfy.data.remote.dto

import kotlinx.serialization.Serializable

// Mirrors backend/src/modules/business/interface/dto/business-response.dto.ts (fields the app uses).

@Serializable
data class ListDto<T>(val data: List<T>)

@Serializable
data class ConnectUrlDto(val url: String)

/** §1: `client` + `redirectUri` are new; the backend redirects to [redirectUri] when OAuth finishes. */
@Serializable
data class StartOAuthRequest(val client: String = "mobile", val redirectUri: String)

@Serializable
data class GbpConnectionDto(
    val id: String,
    val email: String,
    val status: String,
    val error: String? = null,
    val locations: Int = 0,
    val createdAt: String,
)

@Serializable
data class DiscoveredLocationDto(val id: String, val title: String)

@Serializable
data class LocationDiscoveryDto(
    val found: Int,
    val added: List<DiscoveredLocationDto> = emptyList(),
    val restored: List<DiscoveredLocationDto> = emptyList(),
    val already: List<DiscoveredLocationDto> = emptyList(),
    val error: String? = null,
)

@Serializable
data class GbpLocationDto(
    val id: String,
    val title: String,
    val address: String? = null,
    val category: String? = null,
    val rating: Double? = null,
    val totalReviews: Int = 0,
    val needsReply: Int = 0,
    val waiting: Int = 0,
    /** auto | approval | manual | mixed */
    val mode: String,
    val verified: Boolean = true,
    val verificationState: String? = null,
    val connectionEmail: String? = null,
    val lastSyncedAt: String? = null,
    val syncError: String? = null,
    val pausedUntil: String? = null,
)

@Serializable
data class RatingRuleDto(val rating: Int, val mode: String, val templateId: String? = null)

@Serializable
data class LocationSettingsDto(val ratingRules: List<RatingRuleDto> = emptyList())

@Serializable
data class BusinessInfoDto(val title: String, val phone: String? = null, val website: String? = null)

@Serializable
data class GbpLocationDetailDto(
    val id: String,
    val title: String,
    val info: BusinessInfoDto,
    val settings: LocationSettingsDto,
)

@Serializable
data class UpdateLocationRequest(val ratingRules: List<RatingRuleDto>)

@Serializable
data class BusinessReplyViewDto(
    val id: String,
    val text: String,
    val status: String,
    val source: String,
    val templateId: String? = null,
    val auto: Boolean = false,
    val error: String? = null,
    val sendAt: String? = null,
)

@Serializable
data class BusinessReplyTextDto(val text: String, val repliedAt: String? = null)

@Serializable
data class BusinessSentViaDto(val auto: Boolean, val source: String, val sentAt: String? = null)

@Serializable
data class GbpReviewDto(
    val id: String,
    val locationId: String,
    val authorName: String,
    val authorPhoto: String? = null,
    val stars: Int,
    val text: String,
    val reviewedAt: String,
    val reply: BusinessReplyTextDto? = null,
    val sentVia: BusinessSentViaDto? = null,
    /** unreplied | waiting | claude | scheduled | replied | failed | skipped */
    val state: String,
    val draft: BusinessReplyViewDto? = null,
)

@Serializable
data class InboxCountsDto(val pending: Int = 0, val replied: Int = 0, val all: Int = 0)

@Serializable
data class BusinessInboxDto(
    val data: List<GbpReviewDto>,
    val counts: InboxCountsDto,
    val approvable: List<String> = emptyList(),
    /** §5: cursor pagination, new. */
    val nextCursor: String? = null,
)

@Serializable
data class ReplyRequest(val text: String, val templateId: String? = null)

@Serializable
data class AiDraftRequest(val rewrite: Boolean = true, val tone: String = "default")

@Serializable
data class PeriodStatsDto(val replyRate: Double? = null)

@Serializable
data class BusinessAnalyticsDto(val current: PeriodStatsDto)

@Serializable
data class MetricsDto(
    val impressionsMaps: Int = 0,
    val impressionsSearch: Int = 0,
    val calls: Int = 0,
    val websiteClicks: Int = 0,
    val directions: Int = 0,
)

@Serializable
data class BusinessInsightsDto(val current: MetricsDto, val previous: MetricsDto)

@Serializable
data class GbpTemplateDto(
    val id: String,
    val name: String,
    val ratings: List<Int> = emptyList(),
    val hidden: Boolean = false,
    val bodies: List<TemplateBodyDto> = emptyList(),
)

@Serializable
data class TemplateBodyDto(val language: String, val variants: List<String>)

@Serializable
data class VisibilityRequest(val hidden: Boolean)

@Serializable
data class PostCtaDto(val type: String, val url: String? = null)

@Serializable
data class PostEventDto(val title: String, val start: String, val end: String)

@Serializable
data class PostOfferDto(val couponCode: String? = null, val redeemUrl: String? = null, val terms: String? = null)

@Serializable
data class PostContentDto(
    /** STANDARD | EVENT | OFFER */
    val type: String,
    val summary: String,
    val mediaUrl: String? = null,
    val cta: PostCtaDto? = null,
    val event: PostEventDto? = null,
    val offer: PostOfferDto? = null,
)

@Serializable
data class GbpPostDto(
    val id: String,
    val locationId: String,
    val content: PostContentDto,
    /** draft | scheduled | published | failed */
    val status: String,
    val scheduledAt: String? = null,
    val publishedAt: String? = null,
    val error: String? = null,
    val createdAt: String,
)

@Serializable
data class SavePostRequest(
    val locationId: String? = null,
    val content: PostContentDto,
    /** now | schedule | draft */
    val `when`: String,
    val scheduledAt: String? = null,
)

// ---- Proposed endpoints (docs/backend-gaps.md) ----

@Serializable
data class HomeSummaryDto(val health: HealthDto, val setup: SetupDto)

@Serializable
data class HealthDto(val score: Int, val note: String)

@Serializable
data class SetupDto(val steps: List<SetupStepDto>)

@Serializable
data class SetupStepDto(val key: String, val label: String, val done: Boolean)

@Serializable
data class UploadDto(val url: String)

@Serializable
data class NotificationPrefsDto(val newReviews: Boolean, val lowRatings: Boolean, val weeklySummary: Boolean)
