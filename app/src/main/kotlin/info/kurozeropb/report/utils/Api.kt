package info.kurozeropb.report.utils

import info.kurozeropb.report.structures.Report
import info.kurozeropb.report.structures.User

object Api {
    const val baseUrl = "https://reportapp-api.herokuapp.com/v1"
    const val version = "0.0.6"
    const val userAgent = "Report/v$version (https://github.com/reportapp/report)"

    var user: User? = null
    var reports: List<Report>? = null
    var isLoggedin: Boolean = false
}