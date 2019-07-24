package info.kurozeropb.report.utils

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.result.Result
import info.kurozeropb.report.structures.*
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import android.content.Context
import android.os.Build
import kotlinx.coroutines.*
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.serializer

@UnstableDefault
object Api {
    const val baseUrl = "https://reportapp-api.herokuapp.com/v1"

    var userAgent = ""
    var token: String? = null
    var user: User? = null
    var reports: List<ResponseReport>? = null
    var isLoggedin: Boolean = false

    fun getVersions(ctx: Context): Pair<String, String> {
        val packageInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Pair(packageInfo.versionName, String.format("%03d", packageInfo.longVersionCode))
        } else {
            @Suppress("DEPRECATION")
            Pair(packageInfo.versionName, String.format("%03d", packageInfo.versionCode))
        }
    }

    fun fetchApiInfoAsync(): Deferred<Pair<ApiInfoData?, ErrorData?>> {
        return GlobalScope.async {
            val (_, _, result) = Fuel.get("/")
                .timeout(31000)
                .timeoutRead(60000)
                .header(mapOf("Content-Type" to "application/json"))
                .responseJson()

            val (data, error) = result
            when (result) {
                is Result.Failure -> {
                    if (error != null) {
                        val json = String(error.response.data)
                        if (Utils.isJSON(json)) {
                            val parsedError = Json.nonstrict.parse(ErrorResponse.serializer(), json)
                            return@async Pair(null, parsedError.data)
                        } else {
                            return@async Pair(null, ErrorData(error.exception.message ?: "Unkown Error"))
                        }
                    }
                    return@async Pair(null, ErrorData("Unkown Error"))
                }
                is Result.Success -> {
                    if (data != null) {
                        val response = Json.nonstrict.parse(ApiInfoResponse.serializer(), data.content)
                        return@async Pair(response.data, null)
                    }
                    return@async Pair(null, ErrorData("No data returned by the api"))
                }
            }
        }
    }

    /**
     * Request info about the currently logged in user
     * @return [Deferred] Pair with User or ErrorResponse, await in coroutine scope
     */
    fun fetchUserInfoAsync(): Deferred<Pair<User?, ErrorData?>> {
        if (!isLoggedin) {
            return CompletableDeferred(Pair(null, null))
        }

        return GlobalScope.async {
            val (_, _, result) = Fuel.get("/user/@me")
                .timeout(31000)
                .timeoutRead(60000)
                .header(mapOf("Content-Type" to "application/json"))
                .header(mapOf("Authorization" to "Bearer $token"))
                .responseJson()

            val (data, error) = result
            when (result) {
                is Result.Failure -> {
                    if (error != null) {
                        val json = String(error.response.data)
                        if (Utils.isJSON(json)) {
                            val parsedError = Json.nonstrict.parse(ErrorResponse.serializer(), json)
                            return@async Pair(null, parsedError.data)
                        } else {
                            return@async Pair(null, ErrorData(error.exception.message ?: "Unkown Error"))
                        }
                    }
                    return@async Pair(null, ErrorData("Unkown Error"))
                }
                is Result.Success -> {
                    if (data != null) {
                        val response = Json.nonstrict.parse(UserResponse.serializer(), data.content)
                        user = response.data.user

                        val ustr = Json.nonstrict.stringify(User.serializer(), response.data.user)
                        Utils.sharedPreferences.edit().putString("user", ustr).apply()
                        return@async Pair(user, null)
                    }
                    return@async Pair(null, ErrorData("No data returned by the api"))
                }
            }
        }
    }

    /**
     * Request all reports from the api for the currently logged in user
     * @return [Deferred] Pair with List<Report> or ErrorResponse, await in coroutine scope
     */
    fun fetchReportsAsync(): Deferred<Pair<List<ResponseReport>?, ErrorData?>> {
        if (!isLoggedin) {
            return CompletableDeferred(Pair(null, null))
        }

        return GlobalScope.async {
            val (_, _, result) = Fuel.get("/report/all")
                .timeout(31000)
                .timeoutRead(60000)
                .header(mapOf("Content-Type" to "application/json"))
                .header(mapOf("Authorization" to "Bearer $token"))
                .responseJson()

            val (data, error) = result
            when (result) {
                is Result.Failure -> {
                    if (error != null) {
                        val json = String(error.response.data)
                        if (Utils.isJSON(json)) {
                            val parsedError = Json.nonstrict.parse(ErrorResponse.serializer(), json)
                            return@async Pair(null, parsedError.data)
                        } else {
                            return@async Pair(null, ErrorData(error.exception.message ?: "Unkown Error"))
                        }
                    }
                    return@async Pair(null, ErrorData("Unkown Error"))
                }
                is Result.Success -> {
                    if (data != null) {
                        val response = Json.nonstrict.parse(ReportsResponse.serializer(), data.content)
                        reports = response.data.reports

                        val rstr = Json.nonstrict.stringify(ResponseReport.serializer().list, response.data.reports)
                        Utils.sharedPreferences.edit().putString("reports", rstr).apply()
                        return@async Pair(reports, null)
                    }
                    return@async Pair(null, ErrorData("No data returned by the api"))
                }
            }
        }
    }

    /**
     * Fetch a single report
     * @return [Deferred] Pair with Report or ErrorResponse
     */
    fun fetchReportByIdAsync(id: Int): Deferred<Pair<ResponseReport?, ErrorData?>> {
        if (!isLoggedin) {
            return CompletableDeferred(Pair(null, null))
        }

        return GlobalScope.async {
            val (_, _, result) = Fuel.get("/report/$id")
                .timeout(31000)
                .timeoutRead(60000)
                .header(mapOf("Content-Type" to "application/json"))
                .header(mapOf("Authorization" to "Bearer $token"))
                .responseJson()

            val (data, error) = result
            when (result) {
                is Result.Failure -> {
                    if (error != null) {
                        val json = String(error.response.data)
                        if (Utils.isJSON(json)) {
                            val parsedError = Json.nonstrict.parse(ErrorResponse.serializer(), json)
                            return@async Pair(null, parsedError.data)
                        } else {
                            return@async Pair(null, ErrorData(error.exception.message ?: "Unkown Error"))
                        }
                    }
                    return@async Pair(null, ErrorData("Unkown Error"))
                }
                is Result.Success -> {
                    if (data != null) {
                        val response = Json.nonstrict.parse(ReportResponse.serializer(), data.content)
                        return@async Pair(response.data.report, null)
                    }
                    return@async Pair(null, ErrorData("No data returned by the api"))
                }
            }
        }
    }

    fun deleteReportByIdAsync(id: Int): Deferred<Pair<String?, ErrorData?>> {
        if (!isLoggedin) {
            return CompletableDeferred(Pair(null, null))
        }

        return GlobalScope.async {
            val (_, _, result) = Fuel.delete("/report/$id")
                .timeout(31000)
                .timeoutRead(60000)
                .header(mapOf("Content-Type" to "application/json"))
                .header(mapOf("Authorization" to "Bearer $token"))
                .responseJson()

            val (data, error) = result
            when (result) {
                is Result.Failure -> {
                    if (error != null) {
                        val json = String(error.response.data)
                        if (Utils.isJSON(json)) {
                            val parsedError = Json.nonstrict.parse(ErrorResponse.serializer(), json)
                            return@async Pair(null, parsedError.data)
                        } else {
                            return@async Pair(null, ErrorData(error.exception.message ?: "Unkown Error"))
                        }
                    }
                    return@async Pair(null, ErrorData("Unkown Error"))
                }
                is Result.Success -> {
                    if (data != null) {
                        val response = Json.nonstrict.parse(BasicResponse.serializer(), data.content)
                        return@async Pair(response.data.message, null)
                    }
                    return@async Pair(null, ErrorData("No data returned by the api"))
                }
            }
        }
    }

    @ImplicitReflectionSerializer
    fun createReportAsync(report: Report): Deferred<Pair<String?, ErrorData?>> {
        if (!isLoggedin) {
            return CompletableDeferred(Pair(null, null))
        }

        val jsonBody = """
            {
                "feeling": ${report.feeling},
                "note": ${Json.nonstrict.stringify(String.serializer(), report.note)},
                "tags": ${Json.nonstrict.stringify(String.serializer().list, report.tags)}
            }
        """.trimIndent()

        return GlobalScope.async {
            val (_, _, result) = Fuel.post("/report/create")
                .timeout(31000)
                .timeoutRead(60000)
                .header(mapOf("Content-Type" to "application/json"))
                .header(mapOf("Authorization" to "Bearer $token"))
                .body(jsonBody)
                .responseJson()

            val (data, error) = result
            when (result) {
                is Result.Failure -> {
                    if (error != null) {
                        val json = String(error.response.data)
                        if (Utils.isJSON(json)) {
                            val parsedError = Json.nonstrict.parse(ErrorResponse.serializer(), json)
                            return@async Pair(null, parsedError.data)
                        } else {
                            return@async Pair(null, ErrorData(error.exception.message ?: "Unkown Error"))
                        }
                    }
                    return@async Pair(null, ErrorData("Unkown Error"))
                }
                is Result.Success -> {
                    if (data != null) {
                        val response = Json.nonstrict.parse(BasicResponse.serializer(), data.content)
                        return@async Pair(response.data.message, null)
                    }
                    return@async Pair(null, ErrorData("No data returned by the api"))
                }
            }
        }
    }

    fun updateAvatarAsync(url: String): Deferred<Pair<String?, ErrorData?>> {
        if (!isLoggedin) {
            return CompletableDeferred(Pair(null, null))
        }

        val jsonBody = """
            {
                "url": "$url"
            }
        """.trimIndent()

        return GlobalScope.async {
            val (_, _, result) = Fuel.post("/user/avatar")
                .timeout(31000)
                .timeoutRead(60000)
                .header(mapOf("Content-Type" to "application/json"))
                .header(mapOf("Authorization" to "Bearer $token"))
                .body(jsonBody)
                .responseJson()

            val (data, error) = result
            when (result) {
                is Result.Failure -> {
                    if (error != null) {
                        val json = String(error.response.data)
                        if (Utils.isJSON(json)) {
                            val parsedError = Json.nonstrict.parse(ErrorResponse.serializer(), json)
                            return@async Pair(null, parsedError.data)
                        } else {
                            return@async Pair(null, ErrorData(error.exception.message ?: "Unkown Error"))
                        }
                    }
                    return@async Pair(null, ErrorData("Unkown Error"))
                }
                is Result.Success -> {
                    if (data != null) {
                        val response = Json.nonstrict.parse(BasicResponse.serializer(), data.content)
                        return@async Pair(response.data.message, null)
                    }
                    return@async Pair(null, ErrorData("No data returned by the api"))
                }
            }
        }
    }
}