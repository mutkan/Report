package info.kurozeropb.report

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.github.kittinunf.fuel.core.FuelManager
import com.google.android.material.snackbar.Snackbar
import info.kurozeropb.report.structures.Report
import info.kurozeropb.report.structures.User
import info.kurozeropb.report.utils.Api
import info.kurozeropb.report.utils.LocaleHelper
import info.kurozeropb.report.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
// import android.os.Handler
import kotlinx.android.synthetic.main.activity_splash.*
import android.view.animation.AnimationUtils.loadAnimation

@UnstableDefault
class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val splash = findViewById<ImageView>(R.id.splashImage)
        val anim = loadAnimation(this, R.anim.flip_horizontal)
        splash.animation = anim

        val (version, versionCode) = Api.getVersions(this)
        Api.userAgent = "Report/v$version($versionCode) (https://github.com/reportapp/report)"

        // Get saved preferences
        Utils.sharedPreferences = getSharedPreferences("reportapp", Context.MODE_PRIVATE)
        Api.token = Utils.sharedPreferences.getString("token", "")

        // Parse saved userinfo
        val jsonUser = Utils.sharedPreferences.getString("user", "") ?: ""
        Api.user = if (jsonUser.isNotEmpty()) Json.nonstrict.parse(User.serializer(), jsonUser) else null

        // Set logged in
        Api.isLoggedin = Api.token.isNullOrEmpty().not() && Api.user != null

        // Parse saved reports
        val jsonReports = Utils.sharedPreferences.getString("reports", "") ?: ""
        Api.reports = if (jsonReports.isNotEmpty()) Json.nonstrict.parse(Report.serializer().list, jsonReports) else null

        // Set base variables for api requests
        FuelManager.instance.basePath = Api.baseUrl
        FuelManager.instance.baseHeaders = mapOf("User-Agent" to Api.userAgent)

        GlobalScope.launch(Dispatchers.Main) {
            if (Api.isLoggedin) {
                val (reports, reportsError) = Api.fetchReportsAsync().await()
                when {
                    reports != null -> Api.reports = reports
                    reportsError != null -> {
                        Utils.showSnackbar(splash_view, reportsError.message, Snackbar.LENGTH_LONG, Utils.SnackbarType.EXCEPTION)
                        return@launch
                    }
                    else -> {
                        Utils.showSnackbar(splash_view, getString(R.string.failed_userinfo), Snackbar.LENGTH_LONG, Utils.SnackbarType.EXCEPTION)
                        return@launch
                    }
                }

                // Handler().postDelayed({
                    val intent = Intent(this@SplashScreenActivity, MainActivity::class.java)
                    intent.putExtra("reports", Json.nonstrict.stringify(Report.serializer().list, Api.reports!!))
                    startActivity(intent)
                    finish()
                // }, 10000)
            } else {
                val intent = Intent(this@SplashScreenActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.updateBaseContextLocale(base))
    }
}