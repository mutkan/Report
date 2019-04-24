package info.kurozeropb.report.structures

import kotlinx.serialization.Serializable

//import com.github.kittinunf.fuel.core.ResponseDeserializable
//import com.google.gson.Gson

@Serializable
data class Report(
        val rid: Int,
        val tags: List<String>,
        val feeling: Int,
        val note: String,
        val createdAt: String
)

@Serializable
data class ReportsData(
        val message: String,
        val reports: List<Report>
)

@Serializable
data class ReportsResponse(
        override val statusCode: Int,
        override val statusMessage: String,
        val data: ReportsData
) : BaseResponse