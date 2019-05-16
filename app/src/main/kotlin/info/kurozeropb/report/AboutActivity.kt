package info.kurozeropb.report

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.github.pwittchen.swipe.library.rx2.SwipeEvent
import info.kurozeropb.report.utils.Api.version
import info.kurozeropb.report.utils.Utils
import kotlinx.android.synthetic.main.activity_about.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import java.util.*

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        setSupportActionBar(about_toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val observable = Utils.createSwipe()
        Utils.disposable = observable.subscribe { swipeEvent ->
            when (swipeEvent) {
                SwipeEvent.SWIPING_RIGHT -> finish() // Return to main activity when swiped to the right
                else -> return@subscribe
            }
        }

        val year = Calendar.getInstance().get(Calendar.YEAR)
        tv_about.text = HtmlCompat.fromHtml("""
            <p>© $year — <a href="https://kurozeropb.info">Kurozero</a> | Build v$version<br/>
            Created using Kotlin</p>
        """.trimIndent(), HtmlCompat.FROM_HTML_MODE_LEGACY)
        tv_about.movementMethod = LinkMovementMethod.getInstance()

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