package info.kurozeropb.report.structures

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson

data class Report(
        val rid: Int,
        val tags: List<String>,
        val feeling: Int,
        val note: String,
        val createdAt: String
)

data class ReportsData(
        val message: String,
        val reports: List<Report>
)

data class ReportsResponse(
        override val statusCode: Int,
        override val statusMessage: String,
        val data: ReportsData
) : BaseResponse {
    class Deserializer : ResponseDeserializable<ReportsResponse> {
        override fun deserialize(content: String): ReportsResponse? = Gson().fromJson(content, ReportsResponse::class.java)
    }
}