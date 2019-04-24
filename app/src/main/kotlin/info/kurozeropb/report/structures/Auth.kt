package info.kurozeropb.report.structures

import kotlinx.serialization.Serializable

@Serializable
data class AuthData(
        val message: String,
        val token: String
)

@Serializable
data class AuthResponse(
        override val statusCode: Int,
        override val statusMessage: String,
        val data: AuthData
) : BaseResponse