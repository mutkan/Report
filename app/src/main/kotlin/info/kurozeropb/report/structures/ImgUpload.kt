package info.kurozeropb.report.structures

import kotlinx.serialization.Serializable

@Serializable
data class ImgUpload(
    val success: Boolean,
    val hash: String?,
    val shortHash: String?,
    val url: String?
)
