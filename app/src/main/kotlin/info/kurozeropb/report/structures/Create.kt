package info.kurozeropb.report.structures

import kotlinx.serialization.Serializable

@Serializable
data class CreateData(
        val message: String
)

@Serializable
data class CreateResponse(
        override val statusCode: Int,
        override val statusMessage: String,
        val data: CreateData
) : BaseResponse