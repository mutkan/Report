package info.kurozeropb.report

import android.content.Context
import android.os.Bundle
import android.view.MotionEvent
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import com.github.pwittchen.swipe.library.rx2.SwipeEvent
import com.google.android.material.snackbar.Snackbar
import info.kurozeropb.report.structures.Report
import info.kurozeropb.report.utils.Api
import info.kurozeropb.report.utils.LocaleHelper
import info.kurozeropb.report.utils.Utils
import kotlinx.android.synthetic.main.activity_create_report.*
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.UnstableDefault
import org.jetbrains.anko.sdk27.coroutines.onClick

@ImplicitReflectionSerializer
@UnstableDefault
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
        btnCreateReport.onClick {
            val feeling = when (findViewById<RadioButton>(rgFeelings.checkedRadioButtonId)) {
                radioBad -> 0
                radioGood -> 1
                radioExcellent -> 2
                else -> 0
            }

            val tags = etTags.text.split(Regex(", ?"))
            val note = etNotes.text.toString()

            if (tags.isEmpty()) {
                Utils.showSnackbar(create_report_view, getString(R.string.tags_not_empty), Snackbar.LENGTH_LONG, Utils.SnackbarType.EXCEPTION)
                return@onClick
            }

            if (note.isEmpty()) {
                Utils.showSnackbar(create_report_view, getString(R.string.notes_not_empty), Snackbar.LENGTH_LONG, Utils.SnackbarType.EXCEPTION)
                return@onClick
            }

            val report = Report(tags, feeling, note)
            val (res, err) = Api.createReportAsync(report).await()
            when {
                res != null -> {
                    rgFeelings.clearCheck()
                    etTags.text.clear()
                    etNotes.text.clear()
                    Utils.showSnackbar(create_report_view, res, Snackbar.LENGTH_LONG, Utils.SnackbarType.SUCCESS)
                    return@onClick
                }
                err != null -> {
                    Utils.showSnackbar(create_report_view, err.message, Snackbar.LENGTH_LONG, Utils.SnackbarType.EXCEPTION)
                    return@onClick
                }
                else -> {
                    Utils.showSnackbar(create_report_view, getString(R.string.failed_create_report), Snackbar.LENGTH_LONG, Utils.SnackbarType.EXCEPTION)
                    return@onClick
                }
            }
        }
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