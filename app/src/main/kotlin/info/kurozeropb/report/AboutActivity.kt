package info.kurozeropb.report

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.github.pwittchen.swipe.library.rx2.SwipeEvent
import com.google.android.material.snackbar.Snackbar
import info.kurozeropb.report.utils.Api
import info.kurozeropb.report.utils.Utils
import info.kurozeropb.report.utils.Utils.SnackbarType
import kotlinx.android.synthetic.main.activity_about.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.UnstableDefault
import org.jetbrains.anko.sdk27.coroutines.onClick
import java.util.*

@UnstableDefault
class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        setSupportActionBar(about_toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        tv_about.text = getString(R.string.loading)

        val observable = Utils.createSwipe()
        Utils.disposable = observable.subscribe { swipeEvent ->
            when (swipeEvent) {
                SwipeEvent.SWIPING_RIGHT -> finish() // Return to main activity when swiped to the right
                else -> return@subscribe
            }
        }

        val (version, versionCode) = Api.getVersions(this@AboutActivity)
        val year = Calendar.getInstance().get(Calendar.YEAR)

        GlobalScope.launch(Dispatchers.IO) {
            val (info, error) = Api.fetchApiInfoAsync().await()
            when {
                info != null -> {
                    withContext(Dispatchers.Main) {
                        tv_about.text = HtmlCompat.fromHtml("""
                            <p>© $year — <a href="https://kurozeropb.info">Kurozero</a> | Build <b>v$version($versionCode)</b>
                            <br/>
                            Api version <b>v${info.version}</b>, env <b>${info.env}</b></p>
                        """.trimIndent(), HtmlCompat.FROM_HTML_MODE_LEGACY)
                        tv_about.movementMethod = LinkMovementMethod.getInstance()
                    }
                }
                error != null -> {
                    tv_about.text = getString(R.string.tv_show_about, error.message)
                    Utils.showSnackbar(about_view, error.message, Snackbar.LENGTH_LONG, SnackbarType.EXCEPTION)
                    return@launch
                }
            }
        }

        btn_about_back.onClick { finish() }
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        Utils.swipe.dispatchTouchEvent(event)
        return super.dispatchTouchEvent(event)
    }

    override fun onPause() {
        super.onPause()
        if (!Utils.disposable.isDisposed) {
            Utils.disposable.dispose()
        }
    }

    override fun onResume() {
        super.onResume()
        val observable = Utils.createSwipe()
        Utils.disposable = observable.subscribe { swipeEvent ->
            when (swipeEvent) {
                SwipeEvent.SWIPING_RIGHT -> finish() // Return to main activity when swiped to the right
                else -> return@subscribe
            }
        }
    }
}