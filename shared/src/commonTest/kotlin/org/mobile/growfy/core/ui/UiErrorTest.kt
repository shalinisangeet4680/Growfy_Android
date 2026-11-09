package org.mobile.growfy.core.ui

import org.mobile.growfy.core.network.NetworkError
import kotlin.test.Test
import kotlin.test.assertEquals

class UiErrorTest {

    @Test
    fun unknownRouteIsComingSoon() {
        val error = NetworkError.Api(404, "NOT_FOUND", "Cannot GET /api/v1/business/reviews/abc")
        assertEquals(ErrorKind.NotAvailable, error.toUiError().kind)
    }

    @Test
    fun missingRecordKeepsBackendMessage() {
        val ui = NetworkError.Api(404, "NOT_FOUND", "This post doesn't exist.").toUiError()
        assertEquals(ErrorKind.Generic, ui.kind)
        assertEquals("This post doesn't exist.", ui.message)
    }

    @Test
    fun offlineAndTimeoutUseOfflineArt() {
        assertEquals(ErrorKind.Offline, NetworkError.NoConnection.toUiError().kind)
        assertEquals(ErrorKind.Offline, NetworkError.Timeout.toUiError().kind)
    }
}
