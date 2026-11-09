package org.mobile.growfy.domain.model

import kotlin.time.Instant

data class OtpChallenge(val displayPhone: String, val resendInSeconds: Int)

data class Me(val name: String?, val email: String?, val phone: String?, val workspaceName: String) {
    val initials: String
        get() = workspaceName.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }
}

data class GoogleConnection(val id: String, val email: String, val isActive: Boolean, val error: String?, val locationCount: Int)

/** Location-wide reply mode. The backend stores one mode per star rating; [Mixed] means they differ. */
enum class ReplyMode(val api: String) {
    Auto("auto"), Approval("approval"), Manual("manual"), Mixed("mixed");

    companion object {
        fun from(api: String) = entries.firstOrNull { it.api == api } ?: Mixed
    }
}

data class Location(
    val id: String,
    val title: String,
    val address: String?,
    val category: String?,
    val rating: Double?,
    val totalReviews: Int,
    val needsReply: Int,
    val mode: ReplyMode,
    val verified: Boolean,
    val syncError: String?,
)

data class DiscoveredLocation(val id: String, val title: String)

data class ReviewDraft(val id: String, val text: String, val templateId: String?)

data class Review(
    val id: String,
    val locationId: String,
    val authorName: String,
    val authorPhoto: String?,
    val stars: Int,
    val text: String,
    val reviewedAt: Instant,
    val replyText: String?,
    val repliedAutomatically: Boolean,
    val state: String,
    val draft: ReviewDraft?,
) {
    val isAnswered: Boolean get() = replyText != null
    val authorFirstName: String get() = authorName.substringBefore(' ')
}

enum class InboxFilter(val api: String) { Pending("pending"), Replied("replied") }

data class InboxPage(val reviews: List<Review>, val pending: Int, val replied: Int, val nextCursor: String?)

enum class ReplyTone(val api: String, val label: String) {
    Warmer("warmer", "Warmer"), Shorter("shorter", "Shorter"), Formal("formal", "Formal"),
}

enum class PostType(val api: String, val label: String) {
    Offer("OFFER", "Offer"), Update("STANDARD", "Update"), Event("EVENT", "Event");

    val hasSchedule: Boolean get() = this != Update

    companion object {
        fun from(api: String) = entries.firstOrNull { it.api == api } ?: Update
    }
}

enum class PostStatus(val api: String) {
    Draft("draft"), Scheduled("scheduled"), Published("published"), Failed("failed");

    companion object {
        fun from(api: String) = entries.firstOrNull { it.api == api } ?: Draft
    }
}

enum class CtaType(val api: String, val label: String) {
    LearnMore("LEARN_MORE", "Learn more"),
    Book("BOOK", "Book"),
    Order("ORDER", "Order online"),
    Shop("SHOP", "Buy"),
    SignUp("SIGN_UP", "Sign up"),
    Call("CALL", "Call now");

    /** Call uses the profile's phone number; every other button needs an https link. */
    val needsUrl: Boolean get() = this != Call

    companion object {
        fun from(api: String) = entries.firstOrNull { it.api == api }
    }
}

data class PostCta(val type: CtaType, val url: String?)

/** Offers and events both carry a title and a date range on Google. */
data class PostSchedule(val title: String, val start: Instant, val end: Instant)

data class PostOffer(val couponCode: String?, val redeemUrl: String?, val terms: String?)

data class PostContent(
    val type: PostType,
    val summary: String,
    val mediaUrl: String?,
    val cta: PostCta?,
    val schedule: PostSchedule?,
    val offer: PostOffer?,
)

data class Post(
    val id: String,
    val locationId: String,
    val content: PostContent,
    val status: PostStatus,
    val scheduledAt: Instant?,
    val publishedAt: Instant?,
    val createdAt: Instant,
    val error: String?,
) {
    /** The moment the post belongs to on the calendar. */
    val date: Instant get() = publishedAt ?: scheduledAt ?: createdAt
}

enum class PublishMode(val api: String) { Now("now"), Schedule("schedule"), Draft("draft") }

data class Template(val id: String, val name: String, val ratings: List<Int>, val hidden: Boolean, val preview: String)

data class Stat(val label: String, val value: Int, val changePercent: Int?)

data class SetupStep(val key: String, val label: String, val done: Boolean)

data class HomeSummary(val healthScore: Int, val healthNote: String, val setup: List<SetupStep>) {
    val nextStep: SetupStep? get() = setup.firstOrNull { !it.done }
}

data class NotificationPrefs(val newReviews: Boolean, val lowRatings: Boolean, val weeklySummary: Boolean)
