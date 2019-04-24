package info.kurozeropb.report.structures

import kotlinx.serialization.Serializable

@Serializable
data class User(
        val uid: Int,
        val firstName: String,
        val lastName: String,
        val email: String,
        val username: String,
        val createdAt: String
)

@Serializable
data class UserData(
        val message: String,
        val user: User
)

@Serializable
data class UserResponse(
        override val statusCode: Int,
        override val statusMessage: String,
        val data: UserData
) : BaseResponse