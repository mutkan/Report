package info.kurozeropb.report

import android.content.Context
import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.github.pwittchen.swipe.library.rx2.SwipeEvent
import info.kurozeropb.report.utils.LocaleHelper
import info.kurozeropb.report.utils.Utils
import kotlinx.android.synthetic.main.activity_create_report.*
import org.jetbrains.anko.sdk27.coroutines.onClick

class CreateReportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_report)
        setSupportActionBar(create_report_toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val observable = Utils.createSwipe()
        Utils.disposable = observable.subscribe { swipeEvent ->
            when (swipeEvent) {
                SwipeEvent.SWIPING_RIGHT -> finish() // Return to main activity when swiped to the right
                else -> return@subscribe
            }
        }

        btn_back.onClick { finish() }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.updateBaseContextLocale(base))
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