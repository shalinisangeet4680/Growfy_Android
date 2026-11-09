package org.mobile.growfy.data.repository

import org.mobile.growfy.data.remote.dto.GbpConnectionDto
import org.mobile.growfy.data.remote.dto.GbpLocationDto
import org.mobile.growfy.data.remote.dto.GbpPostDto
import org.mobile.growfy.data.remote.dto.GbpReviewDto
import org.mobile.growfy.data.remote.dto.GbpTemplateDto
import org.mobile.growfy.data.remote.dto.PostContentDto
import org.mobile.growfy.data.remote.dto.PostCtaDto
import org.mobile.growfy.data.remote.dto.PostEventDto
import org.mobile.growfy.data.remote.dto.PostOfferDto
import org.mobile.growfy.domain.model.CtaType
import org.mobile.growfy.domain.model.GoogleConnection
import org.mobile.growfy.domain.model.Location
import org.mobile.growfy.domain.model.Post
import org.mobile.growfy.domain.model.PostContent
import org.mobile.growfy.domain.model.PostCta
import org.mobile.growfy.domain.model.PostOffer
import org.mobile.growfy.domain.model.PostSchedule
import org.mobile.growfy.domain.model.PostStatus
import org.mobile.growfy.domain.model.PostType
import org.mobile.growfy.domain.model.ReplyMode
import org.mobile.growfy.domain.model.Review
import org.mobile.growfy.domain.model.ReviewDraft
import org.mobile.growfy.domain.model.Template
import kotlin.time.Instant

internal fun GbpConnectionDto.toDomain() = GoogleConnection(id, email, status == "active", error, locations)

internal fun GbpLocationDto.toDomain() = Location(
    id = id,
    title = title,
    address = address,
    category = category,
    rating = rating,
    totalReviews = totalReviews,
    needsReply = needsReply,
    mode = ReplyMode.from(mode),
    verified = verified,
    syncError = syncError,
)

internal fun GbpReviewDto.toDomain() = Review(
    id = id,
    locationId = locationId,
    authorName = authorName,
    authorPhoto = authorPhoto,
    stars = stars,
    text = text,
    reviewedAt = Instant.parse(reviewedAt),
    replyText = reply?.text,
    repliedAutomatically = sentVia?.auto == true,
    state = state,
    // Only an unsent draft is useful to the UI; sent/discarded ones are history.
    draft = draft?.takeIf { it.status in ACTIVE_DRAFT_STATUSES }?.let { ReviewDraft(it.id, it.text, it.templateId) },
)

private val ACTIVE_DRAFT_STATUSES = setOf("pending_approval", "needs_claude", "scheduled", "failed")

internal fun GbpTemplateDto.toDomain() = Template(
    id = id,
    name = name,
    ratings = ratings,
    hidden = hidden,
    preview = bodies.firstOrNull()?.variants?.firstOrNull().orEmpty(),
)

internal fun GbpPostDto.toDomain() = Post(
    id = id,
    locationId = locationId,
    content = content.toDomain(),
    status = PostStatus.from(status),
    scheduledAt = scheduledAt?.let(Instant::parse),
    publishedAt = publishedAt?.let(Instant::parse),
    createdAt = Instant.parse(createdAt),
    error = error,
)

internal fun PostContentDto.toDomain() = PostContent(
    type = PostType.from(type),
    summary = summary,
    mediaUrl = mediaUrl,
    cta = cta?.let { dto -> CtaType.from(dto.type)?.let { PostCta(it, dto.url) } },
    schedule = event?.let { PostSchedule(it.title, Instant.parse(it.start), Instant.parse(it.end)) },
    offer = offer?.let { PostOffer(it.couponCode, it.redeemUrl, it.terms) },
)

internal fun PostContent.toDto() = PostContentDto(
    type = type.api,
    summary = summary.trim(),
    mediaUrl = mediaUrl,
    cta = cta?.let { PostCtaDto(it.type.api, if (it.type.needsUrl) it.url?.trim() else null) },
    event = schedule?.takeIf { type.hasSchedule }?.let { PostEventDto(it.title.trim(), it.start.toString(), it.end.toString()) },
    offer = offer?.takeIf { type == PostType.Offer }?.let { PostOfferDto(it.couponCode, it.redeemUrl, it.terms) },
)
