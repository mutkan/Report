package info.kurozeropb.report

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import info.kurozeropb.report.utils.LocaleHelper
import kotlinx.serialization.UnstableDefault

@UnstableDefault
class SplashScreenActivity : AppCompatActivity() {
    private val SPLASH_TIME_OUT: Long = 3000 // 3 sec
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler().postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, SPLASH_TIME_OUT)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.updateBaseContextLocale(base))
    }
}