package info.kurozeropb.report.structures

//import com.github.kittinunf.fuel.core.ResponseDeserializable
//import com.google.gson.Gson
import kotlinx.serialization.Serializable

interface BaseResponse {
    val statusCode: Int
    val statusMessage: String
}

@Serializable
data class ErrorData(
        val message: String
)

@Serializable
data class ErrorResponse(
        override val statusCode: Int,
        override val statusMessage: String,
        val data: ErrorData
) : BaseResponse //{
//    class Deserializer : ResponseDeserializable<ErrorResponse> {
//        override fun deserialize(content: String): ErrorResponse? = Gson().fromJson(content, ErrorResponse::class.java)
//    }
//}