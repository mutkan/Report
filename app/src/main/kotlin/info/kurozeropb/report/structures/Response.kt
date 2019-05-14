package info.kurozeropb.report.structures

import kotlinx.serialization.Serializable

interface BaseResponse {
    val statusCode: Int
    val statusMessage: String
}

@Serializable
data class BasicResponseData(
        val message: String
)

@Serializable
data class BasicResponse(
        override val statusCode: Int,
        override val statusMessage: String,
        val data: BasicResponseData
) : BaseResponse

@Serializable
data class ErrorData(
        val message: String
)

@Serializable
data class ErrorResponse(
        override val statusCode: Int,
        override val statusMessage: String,
        val data: ErrorData
) : BaseResponse