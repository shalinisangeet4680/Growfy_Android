package org.mobile.growfy.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class OtpRequest(val phone: String)

@Serializable
data class OtpRequestedDto(val phone: String, val expiresInSeconds: Int, val resendInSeconds: Int)

@Serializable
data class OtpVerifyRequest(val phone: String, val code: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class TokensDto(
    val accessToken: String? = null,
    val accessTokenExpiresIn: Int? = null,
    val refreshToken: String? = null,
    val refreshTokenExpiresIn: Int? = null,
)

@Serializable
data class UserProfileDto(
    val id: String,
    val phone: String? = null,
    val name: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
)

@Serializable
data class WorkspaceSummaryDto(val id: String, val name: String, val role: String)

@Serializable
data class SignInResponseDto(
    val user: UserProfileDto,
    val workspace: WorkspaceSummaryDto,
    val isNewUser: Boolean,
    val accessToken: String? = null,
    val refreshToken: String? = null,
)

@Serializable
data class MeResponseDto(val user: UserProfileDto, val workspace: WorkspaceSummaryDto)
