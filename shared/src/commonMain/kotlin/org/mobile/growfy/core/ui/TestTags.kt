package org.mobile.growfy.core.ui

/**
 * Stable `Modifier.testTag` ids. On Android they're exposed as resource-ids
 * (`testTagsAsResourceId` in MainActivity), so `scripts/ui.sh tap <tag>` and UI tests can find them.
 * See docs/testing.md.
 */
object TestTags {
    const val LOADING = "loading_state"
    const val ERROR = "error_state"
    const val ERROR_RETRY = "error_retry"
    const val EMPTY = "empty_state"

    const val LOGIN_PHONE = "login_phone"
    const val LOGIN_CODE = "login_code"
    const val LOGIN_SUBMIT = "login_submit"

    fun tab(name: String) = "tab_$name"

    const val HOME_LOCATION = "home_location"
    const val HOME_FOCUS_SEND = "home_focus_send"
    const val HOME_FOCUS_EDIT = "home_focus_edit"

    const val REVIEWS_SEARCH = "reviews_search"
    const val REVIEWS_WAITING = "reviews_filter_waiting"
    const val REVIEWS_REPLIED = "reviews_filter_replied"
    const val REVIEWS_TIP_DISMISS = "reviews_tip_dismiss"
    fun reviewRow(index: Int) = "review_row_$index"

    const val REPLY_TEXT = "reply_text"
    const val REPLY_SEND = "reply_send"
    const val REPLY_SKIP = "reply_skip"
    fun replyTone(name: String) = "reply_tone_$name"

    const val POSTS_NEW = "posts_new"
    const val POSTS_CALENDAR = "posts_calendar"
    fun postCard(index: Int) = "post_card_$index"
    fun postFilter(name: String) = "posts_filter_$name"

    const val COMPOSE_SUMMARY = "compose_summary"
    const val COMPOSE_PUBLISH = "compose_publish"
    const val COMPOSE_DRAFT = "compose_draft"
    const val COMPOSE_CLOSE = "compose_close"

    fun locationMode(index: Int, mode: String) = "location_${index}_$mode"
    const val MORE_TEMPLATES = "more_templates"
    const val MORE_ACCOUNTS = "more_accounts"
    const val MORE_NOTIFICATIONS = "more_notifications"
    const val MORE_SIGN_OUT = "more_sign_out"
}
