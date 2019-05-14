package info.kurozeropb.report

import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.github.pwittchen.swipe.library.rx2.Swipe
import com.github.pwittchen.swipe.library.rx2.SwipeEvent
import info.kurozeropb.report.structures.Report
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_show_report.*
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json

@UnstableDefault
class ShowReportActivity : AppCompatActivity() {
    private lateinit var swipe: Swipe
    private lateinit var disposable: Disposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_report)
        setSupportActionBar(show_report_toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val reportString = intent.getStringExtra("report")
        val report = Json.nonstrict.parse(Report.serializer(), reportString)

        tv_show_note.text = getString(R.string.tv_show_note, report.note)

        btn_report_back.setOnClickListener {
            finish()
        }

        swipe = Swipe(500, 500)
        disposable = swipe.observe()
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { swipeEvent ->
                when (swipeEvent) {
                    SwipeEvent.SWIPING_RIGHT -> finish() // Return to main activity when swiped to the right
                    else -> return@subscribe
                }
            }
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        swipe.dispatchTouchEvent(event)
        return super.dispatchTouchEvent(event)
    }

    override fun onPause() {
        super.onPause()
        if (!disposable.isDisposed) {
            disposable.dispose()
        }
    }
}