package info.kurozeropb.report

import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.result.Result
import com.github.pwittchen.swipe.library.rx2.SwipeEvent
import com.google.android.material.snackbar.Snackbar
import info.kurozeropb.report.structures.*
import info.kurozeropb.report.utils.Api
import info.kurozeropb.report.utils.Utils
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
        var report = Json.nonstrict.parse(Report.serializer(), reportString)

        tv_show_note.text = getString(R.string.tv_show_note, "Loading...")
        tv_show_tags.text = getString(R.string.tv_show_tags, "Loading...")

        GlobalScope.launch(Dispatchers.IO) {
            report = fetchReportByIdAsync(report.rid).await() ?: report

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

    /**
     * Fetch a single report
     * @return [Deferred] report
     */
    private fun fetchReportByIdAsync(id: Int): Deferred<Report?> {
        if (!Api.isLoggedin) {
            return CompletableDeferred(null)
        }

        return GlobalScope.async {
            val (_, _, result) = Fuel.get("/report/$id")
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
                            Utils.showSnackbar(show_report_view, applicationContext, errorResponse.data.message, Snackbar.LENGTH_LONG)
                        } else {
                            Utils.showSnackbar(show_report_view, applicationContext, error.message ?: "Unkown Error", Snackbar.LENGTH_LONG)
                        }
                    }
                    return@async null
                }
                is Result.Success -> {
                    if (data != null) {
                        val response = Json.nonstrict.parse(ReportResponse.serializer(), data.content)
                        return@async response.data.report
                    }
                    return@async null
                }
            }
        }
    }
}