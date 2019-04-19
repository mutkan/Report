package info.kurozeropb.report

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.result.Result
import info.kurozeropb.report.structures.AuthResponse
import info.kurozeropb.report.structures.ErrorResponse
import info.kurozeropb.report.structures.User
import kotlinx.android.synthetic.main.activity_scrolling.*
import kotlinx.android.synthetic.main.login_dialog.view.*
import org.jetbrains.anko.doAsync

const val baseUrl = "https://reportapp-api.herokuapp.com/v1"
const val version = "0.0.1"
const val userAgent = "Report/v$version (https://github.com/reportapp/report)"

lateinit var sharedPreferences: SharedPreferences
lateinit var token: String

var user: User? = null
var isLoggedin: Boolean = false

class ScrollingActivity : AppCompatActivity() {

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scrolling)
        setSupportActionBar(toolbar)

        sharedPreferences = getSharedPreferences("reportapp", Context.MODE_PRIVATE)
        token = sharedPreferences.getString("token", "") ?: ""

        val userstr = sharedPreferences.getString("user", "") ?: ""
        println(userstr)
        user = if (userstr.isNotEmpty())
            User.Deserializer().deserialize(userstr)
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
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).show()
        }

        btn_login.setOnClickListener { view ->
            if (btn_login.text == "Logout") {
                token = ""
                user = null
                sharedPreferences.edit().remove("token").apply()
                sharedPreferences.edit().remove("user").apply()
                isLoggedin = false
                btn_login.text = getString(R.string.login_out, "Login")
                return@setOnClickListener
            }

            val loginFactory = LayoutInflater.from(this)
            val loginView = loginFactory.inflate(R.layout.login_dialog, null)

            val loginDialog = AlertDialog.Builder(this)
                    .setView(loginView)
                    .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
                    .setNeutralButton("Register") { dialog, _ ->
                        dialog.dismiss()
                        Snackbar.make(view, "Not yet supported", Snackbar.LENGTH_LONG).show()
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
                                                    val errorResponse = ErrorResponse.Deserializer().deserialize(error.response.data)
                                                    val message = errorResponse?.statusMessage ?: (error.message ?: "Something went wrong")
                                                    Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
                                                }
                                            }
                                            is Result.Success -> {
                                                if (data != null) {
                                                    val response = AuthResponse.Deserializer().deserialize(data.content)
                                                    if (response != null) {
                                                        val usrstr = User.Serializer().serialize(response.data.user)
                                                        token = response.data.token
                                                        user = response.data.user
                                                        sharedPreferences.edit().putString("token", token).apply()
                                                        sharedPreferences.edit().putString("user", usrstr).apply()
                                                        isLoggedin = true
                                                        btn_login.text = getString(R.string.login_out, "Logout")
                                                    }
                                                }
                                            }
                                        }
                                    }
                        }
                    }
            loginDialog.show()
        }
    }
}
