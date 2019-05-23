package info.kurozeropb.report.utils

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.result.Result
import info.kurozeropb.report.structures.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import android.content.Context
import android.os.Build


@UnstableDefault
object Api {
    const val baseUrl = "https://reportapp-api.herokuapp.com/v1"

    var userAgent = ""
    var token: String? = null
    var user: User? = null
    var reports: List<Report>? = null
    var isLoggedin: Boolean = false

    fun getVersion(ctx: Context): String {
        val packageInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        return packageInfo.versionName
    }

    fun getVersionCode(ctx: Context): String {
        val packageInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            String.format("%03d", packageInfo.longVersionCode)
        } else {
            @Suppress("DEPRECATION")
            String.format("%03d", packageInfo.versionCode)
        }
    }

    /**
     * Request info about the currently logged in user
     * @return [Deferred] Pair with User or ErrorResponse, await in coroutine scope
     */
    fun fetchUserInfoAsync(): Deferred<Pair<User?, ErrorResponse?>> {
        if (!isLoggedin) {
            return CompletableDeferred(Pair(null, null))
        }

        return GlobalScope.async {
            val (_, _, result) = Fuel.get("/user/@me")
                .header(mapOf("Content-Type" to "application/json"))
                .header(mapOf("Authorization" to "Bearer $token"))
                .responseJson()

            val (data, error) = result
            when (result) {
                is Result.Failure -> {
                    if (error != null) {
                        val json = String(error.response.data)
                        if (Utils.isJSON(json)) {
                            return@async Pair(null, Json.nonstrict.parse(ErrorResponse.serializer(), json))
                        }
                    }
                    return@async Pair(null, ErrorResponse(500, "Internal Server Error", ErrorData("Unkown Error")))
                }
                is Result.Success -> {
                    if (data != null) {
                        val response = Json.nonstrict.parse(UserResponse.serializer(), data.content)
                        user = response.data.user

                        val ustr = Json.nonstrict.stringify(User.serializer(), response.data.user)
                        Utils.sharedPreferences.edit().putString("user", ustr).apply()
                        return@async Pair(user, null)
                    }
                    return@async Pair(null, ErrorResponse(500, "Internal Server Error", ErrorData("No data returned by the api")))
                }
            }
        }
    }

    /**
     * Request all reports from the api for the currently logged in user
     * @return [Deferred] Pair with List<Report> or ErrorResponse, await in coroutine scope
     */
    fun fetchReportsAsync(): Deferred<Pair<List<Report>?, ErrorResponse?>> {
        if (!isLoggedin) {
            return CompletableDeferred(Pair(null, null))
        }

        return GlobalScope.async {
            val (_, _, result) = Fuel.get("/report/all")
                .header(mapOf("Content-Type" to "application/json"))
                .header(mapOf("Authorization" to "Bearer $token"))
                .responseJson()

            val (data, error) = result
            when (result) {
                is Result.Failure -> {
                    if (error != null) {
                        val json = String(error.response.data)
                        if (Utils.isJSON(json)) {
                            return@async Pair(null, Json.nonstrict.parse(ErrorResponse.serializer(), json))
                        }
                    }
                    return@async Pair(null, ErrorResponse(500, "Internal Server Error", ErrorData("Unkown Error")))
                }
                is Result.Success -> {
                    if (data != null) {
                        val response = Json.nonstrict.parse(ReportsResponse.serializer(), data.content)
                        reports = response.data.reports

                        val rstr = Json.nonstrict.stringify(Report.serializer().list, response.data.reports)
                        Utils.sharedPreferences.edit().putString("reports", rstr).apply()
                        return@async Pair(reports, null)
                    }
                    return@async Pair(null, ErrorResponse(500, "Internal Server Error", ErrorData("No data returned by the api")))
                }
            }
        }
    }

    /**
     * Fetch a single report
     * @return [Deferred] Pair with Report or ErrorResponse
     */
    fun fetchReportByIdAsync(id: Int): Deferred<Pair<Report?, ErrorResponse?>> {
        if (!isLoggedin) {
            return CompletableDeferred(Pair(null, null))
        }

        return GlobalScope.async {
            val (_, _, result) = Fuel.get("/report/$id")
                .header(mapOf("Content-Type" to "application/json"))
                .header(mapOf("Authorization" to "Bearer $token"))
                .responseJson()

            val (data, error) = result
            when (result) {
                is Result.Failure -> {
                    if (error != null) {
                        val json = String(error.response.data)
                        if (Utils.isJSON(json)) {
                            return@async Pair(null, Json.nonstrict.parse(ErrorResponse.serializer(), json))
                        }
                    }
                    return@async Pair(null, ErrorResponse(500, "Internal Server Error", ErrorData("Unkown Error")))
                }
                is Result.Success -> {
                    if (data != null) {
                        val response = Json.nonstrict.parse(ReportResponse.serializer(), data.content)
                        return@async Pair(response.data.report, null)
                    }
                    return@async Pair(null, ErrorResponse(500, "Internal Server Error", ErrorData("No data returned by the api")))
                }
            }
        }
    }
}