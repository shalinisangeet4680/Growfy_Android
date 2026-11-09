package org.mobile.growfy.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import org.mobile.growfy.data.remote.dto.MeResponseDto
import org.mobile.growfy.data.remote.dto.OtpRequest
import org.mobile.growfy.data.remote.dto.OtpRequestedDto
import org.mobile.growfy.data.remote.dto.OtpVerifyRequest
import org.mobile.growfy.data.remote.dto.SignInResponseDto

class AuthApi(private val client: HttpClient) {
    suspend fun requestOtp(phone: String): OtpRequestedDto =
        client.post("auth/otp/request") { setBody(OtpRequest(phone)) }.body()

    suspend fun verifyOtp(phone: String, code: String): SignInResponseDto =
        client.post("auth/otp/verify") { setBody(OtpVerifyRequest(phone, code)) }.body()

    suspend fun logout() {
        client.post("auth/logout")
    }

    suspend fun me(): MeResponseDto = client.get("me").body()
}
