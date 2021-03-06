package info.kurozeropb.report.structures

import kotlinx.serialization.Serializable

@Serializable
data class ImgUpload(
    val success: Boolean,
    val hash: String? = null,
    val shortHash: String? = null,
    val url: String? = null
)
