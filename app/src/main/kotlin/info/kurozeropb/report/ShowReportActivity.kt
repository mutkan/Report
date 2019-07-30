package info.kurozeropb.report

import android.content.Context
import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.github.pwittchen.swipe.library.rx2.SwipeEvent
import com.google.android.material.snackbar.Snackbar
import info.kurozeropb.report.structures.*
import info.kurozeropb.report.utils.Api
import info.kurozeropb.report.utils.LocaleHelper
import info.kurozeropb.report.utils.Utils
import info.kurozeropb.report.utils.Utils.SnackbarType
import kotlinx.android.synthetic.main.activity_show_report.*
import kotlinx.coroutines.*
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import org.jetbrains.anko.sdk27.coroutines.onClick

@UnstableDefault
class ShowReportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_report)
        setSupportActionBar(show_report_toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val reportString = intent.getStringExtra("report")
        var report = Json.nonstrict.parse(ResponseReport.serializer(), reportString)

        tv_show_note.text = getString(R.string.loading)
        tv_show_tags.text = getString(R.string.loading)

        GlobalScope.launch(Dispatchers.IO) {
            val reportResponse = Api.fetchReportByIdAsync(report.rid).await()
            val (rep, error) = reportResponse
            report = when {
                rep != null -> rep
                error != null -> {
                    Utils.showSnackbar(show_report_view, error.message, Snackbar.LENGTH_LONG, SnackbarType.EXCEPTION)
                    return@launch
                }
                else -> report
            }

            withContext(Dispatchers.Main) {
                tv_show_note.text = getString(R.string.tv_show_note, report.note)
                tv_show_tags.text = getString(R.string.tv_show_tags, report.tags.joinToString(", "))
            }
        }

        val observable = Utils.createSwipe()
        Utils.disposable = observable.subscribe { swipeEvent ->
            when (swipeEvent) {
                SwipeEvent.SWIPING_RIGHT -> finish() // Return to main activity when swiped to the right
                else -> return@subscribe
            }
        }

        btn_report_back.onClick { finish() }
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