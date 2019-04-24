package info.kurozeropb.report

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Point
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.result.Result
import kotlinx.android.synthetic.main.activity_scrolling.*
import kotlinx.android.synthetic.main.content_scrolling.*
import kotlinx.android.synthetic.main.login_dialog.view.*
import org.jetbrains.anko.doAsync
import android.widget.LinearLayout
import android.widget.TextView
import info.kurozeropb.report.structures.*
import info.kurozeropb.report.utils.Utils
import kotlinx.android.synthetic.main.register_dialog.view.*
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.list

const val baseUrl = "https://reportapp-api.herokuapp.com/v1"
const val version = "0.0.2"
const val userAgent = "Report/v$version (https://github.com/reportapp/report)"

lateinit var sharedPreferences: SharedPreferences
lateinit var token: String

var user: User? = null
var reports: List<Report>? = null
var isLoggedin: Boolean = false

@UnstableDefault
class ScrollingActivity : AppCompatActivity() {

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scrolling)
        setSupportActionBar(toolbar)

        sharedPreferences = getSharedPreferences("reportapp", Context.MODE_PRIVATE)
        token = sharedPreferences.getString("token", "") ?: ""

        val userstr = sharedPreferences.getString("user", "") ?: ""
        user = if (userstr.isNotEmpty())
            Json.nonstrict.parse(User.serializer(), userstr)
        else null

        val rstr = sharedPreferences.getString("reports", "") ?: ""
        reports = if (rstr.isNotEmpty())
            Json.nonstrict.parse(Report.serializer().list, rstr)
        else null

        isLoggedin = token.isNotEmpty() && user != null

        FuelManager.instance.basePath = baseUrl
        FuelManager.instance.baseHeaders = mapOf("User-Agent" to userAgent)

        btn_login.text =
                if (isLoggedin)
                    getString(R.string.login_out, "Logout")
                else
                    getString(R.string.login_out, "Login")

        fab.setOnClickListener { view ->
            if (isLoggedin.not()) {
                Snackbar.make(view, "Login before creating a report", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val intent = Intent(this, CreateReportActivity::class.java)
            startActivity(intent)
        }

        btn_login.setOnClickListener { view ->
            if (btn_login.text == "Logout") {
                token = ""
                user = null
                sharedPreferences.edit().remove("token").apply()
                sharedPreferences.edit().remove("user").apply()
                isLoggedin = false
                btn_login.text = getString(R.string.login_out, "Login")
                scrollLayout.removeAllViews()
                return@setOnClickListener
            }

            val loginFactory = LayoutInflater.from(this)
            val loginView = loginFactory.inflate(R.layout.login_dialog, null)

            val loginDialog = AlertDialog.Builder(this)
                    .setView(loginView)
                    .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
                    .setNeutralButton("Register") { dialog, _ ->
                        dialog.dismiss()

                        val registerFactory = LayoutInflater.from(this)
                        val registerView = registerFactory.inflate(R.layout.register_dialog, null)
                        val registerDialog = AlertDialog.Builder(this)
                                .setView(registerView)
                                .setNegativeButton("Cancel") { d, _ -> d.cancel() }
                                .setPositiveButton("Confirm") { _, _ ->  }.create()
                        registerDialog.show()
                        registerDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            // Input checks
                            if (registerView.firstInput.text.isNullOrEmpty()
                                    || registerView.lastInput.text.isNullOrEmpty()
                                    || registerView.userInput.text.isNullOrEmpty()
                                    || registerView.passInput.text.isNullOrEmpty()
                                    || registerView.cPassInput.text.isNullOrEmpty()
                                    || registerView.emailInput.text.isNullOrEmpty()
                            ) {
                                Snackbar.make(registerView, "Please complete all fields", Snackbar.LENGTH_LONG).show()
                            } else if (registerView.passInput.text.length < 4) {
                                Snackbar.make(registerView, "Password needs to be atleast 4 characters long", Snackbar.LENGTH_LONG).show()
                            } else if (registerView.passInput.text.toString() != registerView.cPassInput.text.toString()) {
                                Snackbar.make(registerView, "Passwords do not match", Snackbar.LENGTH_LONG).show()
                            } else {
                                val reqbody = """
                                    {
                                        "firstName": "${registerView.firstInput.text}",
                                        "lastName": "${registerView.lastInput.text}",
                                        "username": "${registerView.userInput.text}",
                                        "password": "${registerView.passInput.text}",
                                        "email": "${registerView.emailInput.text}"
                                    }
                                """.trimMargin()

                                registerUser(reqbody)
                                registerDialog.dismiss()
                            }
                        }
                    }
                    .setPositiveButton("Login") { _, _ ->
                        if (loginView.usernameInput.text.isNullOrEmpty() || loginView.passwordInput.text.isNullOrEmpty()) {
                            Snackbar.make(view, "Please complete all fields", Snackbar.LENGTH_LONG).show()
                            return@setPositiveButton
                        }

                        doAsync {
                            val reqbody = "{\"username\": \"${loginView.usernameInput.text}\", \"password\": \"${loginView.passwordInput.text}\"}"
                            Fuel.post("/auth/login")
                                    .header(mapOf("Content-Type" to "application/json"))
                                    .body(reqbody)
                                    .responseJson { _, _, result ->
                                        val (data, error) = result
                                        when (result) {
                                            is Result.Failure -> {
                                                if (error != null) {
                                                    btn_login.text = getString(R.string.login_out, "Login")
                                                    val json = String(error.response.data)
                                                    if (Utils.isJSON(json)) {
                                                        val errorResponse = Json.nonstrict.parse(ErrorResponse.serializer(), json)
                                                        Snackbar.make(view, errorResponse.data.message, Snackbar.LENGTH_LONG).show()
                                                    } else {
                                                        Snackbar.make(view, error.message ?: "Unkown Error", Snackbar.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                            is Result.Success -> {
                                                if (data != null) {
                                                    val response = Json.nonstrict.parse(AuthResponse.serializer(), data.content)
                                                    token = response.data.token
                                                    sharedPreferences.edit().putString("token", token).apply()

                                                    isLoggedin = true
                                                    btn_login.text = getString(R.string.login_out, "Logout")

                                                    fetchReports()
                                                    fetchUserInfo()
                                                }
                                            }
                                        }
                                    }
                        }
                    }.create()
            loginDialog.show()
        }

        swipeContainer.setOnRefreshListener {
            fetchReports()
        }

        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright, android.R.color.holo_green_light,  android.R.color.holo_orange_light, android.R.color.holo_red_light)
    }

    override fun onStart() {
        super.onStart()

        // Load reports if user is still logged in
        if (isLoggedin) {
            fetchReports()
        }
    }

    private fun registerUser(userInfo: String) {
        doAsync {
            Fuel.post("/auth/register")
                    .header(mapOf("Content-Type" to "application/json"))
                    .body(userInfo)
                    .responseJson { _, _, result ->
                        val (data, error) = result
                        when (result) {
                            is Result.Failure -> {
                                if (error != null) {
                                    val json = String(error.response.data)
                                    if (Utils.isJSON(json)) {
                                        val errorResponse = Json.nonstrict.parse(ErrorResponse.serializer(), json)
                                        Snackbar.make(main_view, errorResponse.data.message, Snackbar.LENGTH_LONG).show()
                                    } else {
                                        Snackbar.make(main_view, error.message ?: "Unkown Error", Snackbar.LENGTH_LONG).show()
                                    }
                                }
                            }
                            is Result.Success -> {
                                if (data != null) {
                                    val response = Json.nonstrict.parse(BasicResponse.serializer(), data.content)
                                    Snackbar.make(main_view, response.data.message, Snackbar.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
        }
    }

    private fun fetchUserInfo() {
        if (isLoggedin.not()) {
            return
        }

        doAsync {
            Fuel.get("/user/@me")
                    .header(mapOf("Content-Type" to "application/json"))
                    .header(mapOf("Authorization" to "Bearer $token"))
                    .responseJson { _, _, result ->
                        val (data, error) = result
                        when (result) {
                            is Result.Failure -> {
                                if (error != null) {
                                    val json = String(error.response.data)
                                    if (Utils.isJSON(json)) {
                                        val errorResponse = Json.nonstrict.parse(ErrorResponse.serializer(), json)
                                        Snackbar.make(main_view, errorResponse.data.message, Snackbar.LENGTH_LONG).show()
                                    } else {
                                        Snackbar.make(main_view, error.message ?: "Unkown Error", Snackbar.LENGTH_LONG).show()
                                    }
                                }
                            }
                            is Result.Success -> {
                                if (data != null) {
                                    val response = Json.nonstrict.parse(UserResponse.serializer(), data.content)
                                    user = response.data.user
                                    val ustr = Json.nonstrict.stringify(User.serializer(), response.data.user)
                                    sharedPreferences.edit().putString("user", ustr).apply()
                                }
                            }
                        }
                    }
        }
    }

    private fun fetchReports() {
        if (isLoggedin.not()) {
            swipeContainer.isRefreshing = false
            return
        }

        scrollLayout.removeAllViews()

        doAsync {
            Fuel.get("/report/all")
                    .header(mapOf("Content-Type" to "application/json"))
                    .header(mapOf("Authorization" to "Bearer $token"))
                    .responseJson { _, _, result ->
                        val (data, error) = result
                        when (result) {
                            is Result.Failure -> {
                                if (error != null) {
                                    val json = String(error.response.data)
                                    if (Utils.isJSON(json)) {
                                        val errorResponse = Json.nonstrict.parse(ErrorResponse.serializer(), json)
                                        Snackbar.make(main_view, errorResponse.data.message, Snackbar.LENGTH_LONG).show()
                                    } else {
                                        Snackbar.make(main_view, error.message ?: "Unkown Error", Snackbar.LENGTH_LONG).show()
                                    }
                                }
                                swipeContainer.isRefreshing = false
                            }
                            is Result.Success -> {
                                if (data != null) {
                                    val response = Json.nonstrict.parse(ReportsResponse.serializer(), data.content)
                                    reports = response.data.reports
                                    val rstr = Json.nonstrict.stringify(Report.serializer().list, response.data.reports)
                                    sharedPreferences.edit().putString("reports", rstr).apply()
                                    loadReports(response.data.reports)
                                }
                                swipeContainer.isRefreshing = false
                            }
                        }
                    }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadReports(reports: List<Report>?) {
        if (reports != null) {
            scrollLayout.removeAllViews()

            val display = windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)
            val width = size.x

            for (report in reports) {
                // Create new cardview and add to scrolllayout
                val cardView = CardView(ContextThemeWrapper(this, R.style.CardViewStyle), null, 0)
                val cardInner = LinearLayout(ContextThemeWrapper(this, R.style.Widget_CardContent))
                cardView.addView(cardInner)
                scrollLayout.addView(cardView)
                scrollLayout.setPadding(0, 50, 0, 0)

                // cardview loses margins and style when inflated so re-add those here
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, 50, 0, 50)
                cardView.layoutParams = params
                cardView.layoutParams.height = 500
                cardView.layoutParams.width = width - 150
                cardView.setContentPadding(5, 5, 5, 5)
                cardView.useCompatPadding = true

                val tvTitle = TextView(this)
                tvTitle.textSize = 24f
                tvTitle.text = "Title"

                val tvNote = TextView(this)
                tvNote.text = report.note

                cardInner.addView(tvTitle)
                cardInner.addView(tvNote)
            }
        }
    }
}
