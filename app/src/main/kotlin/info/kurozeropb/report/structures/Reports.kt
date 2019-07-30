package info.kurozeropb.report.structures

import kotlinx.serialization.Serializable

data class Report(
        val tags: List<String>,
        val feeling: Int,
        val note: String
)

@Serializable
data class ResponseReport(
        val rid: String,
        val tags: List<String>,
        val feeling: Int,
        val note: String,
        val createdAt: String
)

@Serializable
data class ReportData(
        val message: String,
        val report: ResponseReport
)

@Serializable
data class ReportResponse(
        override val statusCode: Int,
        override val statusMessage: String,
        val data: ReportData
) : BaseResponse

@Serializable
data class ReportsData(
        val message: String,
        val reports: List<ResponseReport>
)

@Serializable
data class ReportsResponse(
        override val statusCode: Int,
        override val statusMessage: String,
        val data: ReportsData
) : BaseResponse