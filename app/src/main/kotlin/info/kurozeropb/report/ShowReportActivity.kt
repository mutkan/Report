package info.kurozeropb.report

import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import info.kurozeropb.report.structures.Report
import kotlinx.android.synthetic.main.activity_show_report.*
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import java.lang.Math.abs

@UnstableDefault
class ShowReportActivity : AppCompatActivity() {
    private var x1: Float? = 0.toFloat()
    private var x2: Float? = 0.toFloat()
    private var velocityX1: Float? = 0.toFloat()
    private var velocityX2: Float? = 0.toFloat()
    private var flingCount = 0

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
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val action = event?.action
        if (x1 == 0.toFloat()) {
            x1 = event?.rawX
        } else {
            x2 = event?.rawX
            val distanceX: Float? = x1!! - x2!!
            val timeX: Float? = event?.downTime?.toFloat()

            velocityX2 = velocityX1 //v2 = previous v1
            velocityX1 = distanceX!! / timeX!!

            val velocityDelta: Float? = velocityX2!! - velocityX1!!
            velocityX2 = 0.toFloat()
            if (velocityX1!! > 0.toFloat() && velocityX1!! == abs(velocityDelta!!)) { // fling left
                this.flingCount++
                println("fling left!  fling count is: ${this.flingCount}")
                return true
            } else if (velocityX1!! < 0.toFloat() && abs(velocityX1!!) == velocityDelta!! && action == MotionEvent.ACTION_MOVE) { // fling right
                this.flingCount++
                finish() // Swipe left to right to go back
                return true
            }
            x1 = 0.toFloat()
        }

        return super.onTouchEvent(event)
    }
}