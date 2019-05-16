package info.kurozeropb.report

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.result.Result
import com.github.zawadz88.materialpopupmenu.popupMenu
import com.google.android.material.snackbar.Snackbar
import com.wajahatkarim3.easyvalidation.core.view_ktx.validator
import info.kurozeropb.report.structures.*
import info.kurozeropb.report.utils.Api
import info.kurozeropb.report.utils.Utils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_scrolling.*
import kotlinx.android.synthetic.main.login_dialog.*
import kotlinx.android.synthetic.main.login_dialog.view.*
import kotlinx.android.synthetic.main.register_dialog.view.*
import kotlinx.android.synthetic.main.report_card.view.*
import kotlinx.coroutines.*
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import org.jetbrains.anko.sdk27.coroutines.onClick

lateinit var sharedPreferences: SharedPreferences

@UnstableDefault
@SuppressLint("InflateParams")
class MainActivity : AppCompatActivity() {

    private var mainMenu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        // Get saved preferences
        sharedPreferences = getSharedPreferences("reportapp", Context.MODE_PRIVATE)
        Api.token = sharedPreferences.getString("token", "")

        // Parse saved userinfo
        val jsonUser = sharedPreferences.getString("user", "") ?: ""
        Api.user = if (jsonUser.isNotEmpty()) Json.nonstrict.parse(User.serializer(), jsonUser) else null

        // Parse saved reports
        val jsonReports = sharedPreferences.getString("reports", "") ?: ""
        Api.reports = if (jsonReports.isNotEmpty()) Json.nonstrict.parse(Report.serializer().list, jsonReports) else null

        // Set logged in
        Api.isLoggedin = Api.token.isNullOrEmpty().not() && Api.user != null

        // Set base variables for api requests
        FuelManager.instance.basePath = Api.baseUrl
        FuelManager.instance.baseHeaders = mapOf("User-Agent" to Api.userAgent)

        fab.setOnClickListener { view ->
            if (!Api.isLoggedin) {
                Utils.showSnackbar(view, applicationContext, "Login before creating a report", Snackbar.LENGTH_LONG)
                return@setOnClickListener
            }

            val intent = Intent(this@MainActivity, CreateReportActivity::class.java)
            startActivity(intent)
        }

        swipeContainer.setOnRefreshListener {
            GlobalScope.launch(Dispatchers.Main) {
                if (Api.isLoggedin) {
                    val reports = fetchReportsAsync().await()
                    val loaded = loadReports(reports)
                    if (loaded) {
                        swipeContainer.isRefreshing = false
                    }
                } else {
                    swipeContainer.isRefreshing = false
                }
            }
        }

        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright, android.R.color.holo_green_light,  android.R.color.holo_orange_light, android.R.color.holo_red_light)
    }

    override fun onStart() {
        super.onStart()

        // Load reports if user is still logged in
        GlobalScope.launch(Dispatchers.Main) {
            if (Api.isLoggedin && pb_reports != null) {
                pb_reports.visibility = View.VISIBLE
                val reports = fetchReportsAsync().await()

                val loaded = loadReports(reports)
                if (loaded) {
                    pb_reports.visibility = View.GONE
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        mainMenu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.main_menu) {
            val view = checkNotNull(findViewById<View>(R.id.main_menu))

            val popupMenu = popupMenu {
                section {
                    item {
                        label = getString(R.string.login_out, if (Api.isLoggedin) "Logout"  else "Login")
                        icon = if (Api.isLoggedin) R.drawable.logout  else R.drawable.login
                        callback = { if (Api.isLoggedin) logout() else login() }
                    }
                    item {
                        label = "About"
                        icon = R.drawable.information
                        callback = {
                            val intent = Intent(this@MainActivity, AboutActivity::class.java)
                            startActivity(intent)
                        }
                    }
                }
            }

            popupMenu.show(this@MainActivity, view)
            return true
        }
        return false
    }

    /**
     * If logged in, removes the user data and clears all items in the scroll-layout
     */
    private fun logout() {
        if (Api.isLoggedin) {
            Api.token = null
            Api.user = null
            sharedPreferences.edit().remove("token").apply()
            sharedPreferences.edit().remove("user").apply()
            scrollLayout.removeAllViews()
            Api.isLoggedin = false
        }
    }

    /**
     * If not logged in, makes an api request and saves the jwt token it returns
     * than fetches the autheticated user's reports and userinfo
     */
    private fun login() {
        if (Api.isLoggedin) return

        val loginFactory = LayoutInflater.from(this@MainActivity)
        val loginView = loginFactory.inflate(R.layout.login_dialog, null)

        val loginDialog = AlertDialog.Builder(this@MainActivity)
            .setView(loginView)
            .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            .setNeutralButton("Register") { _, _ -> }
            .setPositiveButton("Login") { _, _ -> }.create()
        loginDialog.show()

        // Login dialog neutral button (REGISTER)
        loginDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            val registerFactory = LayoutInflater.from(this@MainActivity)
            val registerView = registerFactory.inflate(R.layout.register_dialog, null)
            val registerDialog = AlertDialog.Builder(this@MainActivity)
                .setView(registerView)
                .setNegativeButton("Cancel") { d, _ -> d.cancel() }
                .setPositiveButton("Confirm") { _, _ ->  }.create()
            registerDialog.show()

            // Register dialog positive button (CONFIRM)
            registerDialog.getButton(AlertDialog.BUTTON_POSITIVE).onClick {
                val validEmail = registerView.emailInput.validator()
                    .validEmail()
                    .nonEmpty()
                    .addErrorCallback { registerView.emailInput.error = it }
                    .addSuccessCallback { registerView.emailInput.error = null }
                    .check()

                val validFirstname = registerView.firstInput.validator()
                    .nonEmpty()
                    .addErrorCallback { registerView.firstInput.error = it }
                    .addSuccessCallback { registerView.firstInput.error = null }
                    .check()

                val validLastname = registerView.lastInput.validator()
                    .nonEmpty()
                    .addErrorCallback { registerView.lastInput.error = it }
                    .addSuccessCallback { registerView.lastInput.error = null }
                    .check()

                val validUsername = registerView.userInput.validator()
                    .nonEmpty()
                    .minLength(4)
                    .addErrorCallback { registerView.userInput.error = it }
                    .addSuccessCallback { registerView.userInput.error = null }
                    .check()

                val validPassword = registerView.passInput.validator()
                    .nonEmpty()
                    .atleastOneUpperCase()
                    .atleastOneSpecialCharacters()
                    .atleastOneNumber()
                    .minLength(4)
                    .addErrorCallback { registerView.passInput.error = it }
                    .addSuccessCallback { registerView.passInput.error = null }
                    .check()

                val validCPassword = registerView.cPassInput.validator()
                    .nonEmpty()
                    .atleastOneUpperCase()
                    .atleastOneSpecialCharacters()
                    .atleastOneNumber()
                    .minLength(4)
                    .addErrorCallback { registerView.cPassInput.error = it }
                    .addSuccessCallback { registerView.cPassInput.error = null }
                    .check()

                // If any of the inputs is not valid, return
                if (!validEmail || !validFirstname || !validLastname || !validUsername || !validPassword || !validCPassword) {
                    return@onClick
                }

                // Check if password and confirm password are the same
                if (registerView.passInput.text.toString() != registerView.cPassInput.text.toString()) {
                    Utils.showSnackbar(registerView, applicationContext, "Passwords do not match", Snackbar.LENGTH_LONG)
                    return@onClick
                }

                val reqbody = """
                    {
                        "firstName": "${registerView.firstInput.text}",
                        "lastName": "${registerView.lastInput.text}",
                        "username": "${registerView.userInput.text}",
                        "password": "${registerView.passInput.text}",
                        "email": "${registerView.emailInput.text}"
                    }
                """.trimMargin()

                launch(Dispatchers.IO) {
                    val (_, _, result) = Fuel.post("/auth/register")
                        .header(mapOf("Content-Type" to "application/json"))
                        .body(reqbody)
                        .responseJson()

                    val (data, error) = result
                    when (result) {
                        is Result.Failure -> {
                            if (error != null) {
                                val json = String(error.response.data)
                                if (Utils.isJSON(json)) {
                                    val errorResponse = Json.nonstrict.parse(ErrorResponse.serializer(), json)
                                    Utils.showSnackbar(registerView, applicationContext, errorResponse.data.message, Snackbar.LENGTH_LONG)
                                } else {
                                    Utils.showSnackbar(registerView, applicationContext, error.message ?: "Unkown Error", Snackbar.LENGTH_LONG)
                                }
                            }
                        }
                        is Result.Success -> {
                            if (data != null) {
                                val response = Json.nonstrict.parse(BasicResponse.serializer(), data.content)
                                withContext(Dispatchers.Main) { registerDialog.dismiss() }
                                Utils.showSnackbar(loginView, applicationContext, response.data.message, Snackbar.LENGTH_LONG)
                            }
                        }
                    }
                }
            }
        }

        // Login dialog positive button (LOGIN)
        loginDialog.getButton(AlertDialog.BUTTON_POSITIVE).onClick {

            val validUsername = loginView.usernameInput.validator()
                .nonEmpty()
                .minLength(4)
                .addErrorCallback { loginView.usernameInput.error = it }
                .addSuccessCallback { loginView.usernameInput.error = null }
                .check()

            val validPassword = loginView.passwordInput.validator()
                .nonEmpty()
                .minLength(4)
                .addErrorCallback { loginView.passwordInput.error = it }
                .addSuccessCallback { loginView.passwordInput.error = null }
                .check()

            if (!validUsername || !validPassword) {
                return@onClick
            }

            loginDialog.pb_login.visibility = View.VISIBLE
            loginDialog.usernameInput.visibility = View.INVISIBLE
            loginDialog.passwordInput.visibility = View.INVISIBLE

            launch(Dispatchers.IO) {
                val reqbody = "{\"username\": \"${loginView.usernameInput.text}\", \"password\": \"${loginView.passwordInput.text}\"}"
                val (_, _, result) = Fuel.post("/auth/login")
                    .header(mapOf("Content-Type" to "application/json"))
                    .body(reqbody)
                    .responseJson()

                val (data, error) = result
                when (result) {
                    is Result.Failure -> {
                        withContext(Dispatchers.Main) {
                            loginDialog.pb_login.visibility = View.GONE
                            loginDialog.usernameInput.visibility = View.VISIBLE
                            loginDialog.passwordInput.visibility = View.VISIBLE
                        }

                        if (error != null) {
                            val json = String(error.response.data)
                            if (Utils.isJSON(json)) {
                                val errorResponse = Json.nonstrict.parse(ErrorResponse.serializer(), json)
                                Utils.showSnackbar(loginView, applicationContext, errorResponse.data.message, Snackbar.LENGTH_LONG)
                            } else {
                                Utils.showSnackbar(loginView, applicationContext, error.message ?: "Unkown Error", Snackbar.LENGTH_LONG)
                            }
                        }
                    }
                    is Result.Success -> {
                        if (data != null) {
                            val response = Json.nonstrict.parse(AuthResponse.serializer(), data.content)
                            Api.token = response.data.token
                            sharedPreferences.edit().putString("token", Api.token ?: "").apply()

                            Api.isLoggedin = true

                            val reports = fetchReportsAsync().await()
                            val user = fetchUserInfoAsync().await()

                            withContext(Dispatchers.Main) {
                                loadReports(reports)
                                loginDialog.dismiss()
                            }

                            val fullName = if (user != null) "${user.firstName} ${user.lastName}" else ""
                            Utils.showSnackbar(main_view, applicationContext, "Welcome $fullName", Snackbar.LENGTH_LONG)
                        }
                    }
                }
            }
        }
    }

    /**
     * Request info about the currently logged in user
     * @return [Deferred] user, await in coroutine scope
     */
    private fun fetchUserInfoAsync(): Deferred<User?> {
        if (!Api.isLoggedin) {
            return CompletableDeferred(null)
        }

        return GlobalScope.async {
            val (_, _, result) = Fuel.get("/user/@me")
                    .header(mapOf("Content-Type" to "application/json"))
                    .header(mapOf("Authorization" to "Bearer ${Api.token}"))
                    .responseJson()

            val (data, error) = result
            when (result) {
                is Result.Failure -> {
                    if (error != null) {
                        val json = String(error.response.data)
                        if (Utils.isJSON(json)) {
                            val errorResponse = Json.nonstrict.parse(ErrorResponse.serializer(), json)
                            Utils.showSnackbar(main_view, applicationContext, errorResponse.data.message, Snackbar.LENGTH_LONG)
                        } else {
                            Utils.showSnackbar(main_view, applicationContext, error.message ?: "Unkown Error", Snackbar.LENGTH_LONG)
                        }
                    }
                    return@async null
                }
                is Result.Success -> {
                    if (data != null) {
                        val response = Json.nonstrict.parse(UserResponse.serializer(), data.content)
                        Api.user = response.data.user

                        val ustr = Json.nonstrict.stringify(User.serializer(), response.data.user)
                        sharedPreferences.edit().putString("user", ustr).apply()
                        return@async Api.user
                    }
                    return@async null
                }
            }
        }
    }

    /**
     * Request all reports from the api for the currently logged in user
     * @return [Deferred] list of reports, await in coroutine scope
     */
    private fun fetchReportsAsync(): Deferred<List<Report>?> {
        if (!Api.isLoggedin) {
            return CompletableDeferred(null)
        }

        return GlobalScope.async {
            val (_, _, result) = Fuel.get("/report/all")
                    .header(mapOf("Content-Type" to "application/json"))
                    .header(mapOf("Authorization" to "Bearer ${Api.token}"))
                    .responseJson()

            val (data, error) = result
            when (result) {
                is Result.Failure -> {
                    if (error != null) {
                        val json = String(error.response.data)
                        if (Utils.isJSON(json)) {
                            val errorResponse = Json.nonstrict.parse(ErrorResponse.serializer(), json)
                            Utils.showSnackbar(main_view, applicationContext, errorResponse.data.message, Snackbar.LENGTH_LONG)
                        } else {
                            Utils.showSnackbar(main_view, applicationContext, error.message ?: "Unkown Error", Snackbar.LENGTH_LONG)
                        }
                    }
                    return@async null
                }
                is Result.Success -> {
                    if (data != null) {
                        val response = Json.nonstrict.parse(ReportsResponse.serializer(), data.content)
                        Api.reports = response.data.reports

                        val rstr = Json.nonstrict.stringify(Report.serializer().list, response.data.reports)
                        sharedPreferences.edit().putString("reports", rstr).apply()
                        return@async Api.reports
                    }
                    return@async null
                }
            }
        }
    }

    /**
     * Load fetched reports into the scroll-layout
     * @return [Boolean]
     */
    private fun loadReports(reports: List<Report>?): Boolean {
        if (reports != null) {
            scrollLayout.removeAllViews()

            /** Sort newest report first */
            val sorted = reports.sortedByDescending { it.rid }

            val display = windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)
            val width = size.x

            val factory = LayoutInflater.from(this)
            for (report in sorted) {
                val cardView = factory.inflate(R.layout.report_card, null)
                scrollLayout.addView(cardView)
                scrollLayout.setPadding(0, 50, 0, 0)

                // cardview loses margins and style when inflated so re-add those here
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, 50, 0, 50)
                cardView.layoutParams = params
                cardView.layoutParams.height = 500
                cardView.layoutParams.width = width - 150

                val note = if (report.note.length > 20)
                    report.note.slice(0..20) + "..."
                else
                    report.note

                cardView.tv_note.text = getString(R.string.tv_note_text, note)
                cardView.tv_created.text = getString(R.string.tv_created_text, Utils.formatISOString(report.createdAt))
                cardView.ib_show.setOnClickListener {
                    val intent = Intent(this, ShowReportActivity::class.java)
                    intent.putExtra("report", Json.nonstrict.stringify(Report.serializer(), report))
                    startActivity(intent)
                }

                cardView.setOnClickListener {
                    val intent = Intent(this, ShowReportActivity::class.java)
                    intent.putExtra("report", Json.nonstrict.stringify(Report.serializer(), report))
                    startActivity(intent)
                }
            }
            return true
        }
        return false
    }
}
