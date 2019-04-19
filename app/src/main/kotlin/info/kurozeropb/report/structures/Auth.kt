package info.kurozeropb.report.structures

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson

data class User(
        val uid: Int,
        val firstName: String,
        val lastName: String,
        val email: String,
        val username: String,
        val createdAt: String
) {
    class Deserializer : ResponseDeserializable<User> {
        override fun deserialize(content: String): User? = Gson().fromJson(content, User::class.java)
    }

    class Serializer {
        fun serialize(content: User): String = Gson().toJson(content, User::class.java)
    }
}

data class AuthData(
        val message: String,
        val token: String,
        val user: User
)

data class AuthResponse(
        override val statusCode: Int,
        override val statusMessage: String,
        val data: AuthData
) : BaseResponse {
    class Deserializer : ResponseDeserializable<AuthResponse> {
        override fun deserialize(content: String): AuthResponse? = Gson().fromJson(content, AuthResponse::class.java)
    }

    class Serializer {
        fun serialize(content: AuthResponse): String = Gson().toJson(content, AuthResponse::class.java)
    }
}