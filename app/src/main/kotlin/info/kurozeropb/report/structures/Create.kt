package info.kurozeropb.report.structures

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson

data class CreateData(
        val message: String
)

data class CreateResponse(
        override val statusCode: Int,
        override val statusMessage: String,
        val data: CreateData
) : BaseResponse {
    class Deserializer : ResponseDeserializable<CreateResponse> {
        override fun deserialize(content: String): CreateResponse? = Gson().fromJson(content, CreateResponse::class.java)
    }
}