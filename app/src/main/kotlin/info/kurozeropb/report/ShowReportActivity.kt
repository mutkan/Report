package info.kurozeropb.report

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import info.kurozeropb.report.structures.Report
import kotlinx.android.synthetic.main.activity_show_report.*
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json

@UnstableDefault
class ShowReportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_report)
        setSupportActionBar(show_report_toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val reportString = intent.getStringExtra("report")
        val report = Json.nonstrict.parse(Report.serializer(), reportString)

        println(report)

        tv_show_note.text = getString(R.string.tv_show_note, report.note)

        btn_report_back.setOnClickListener {
            finish()
        }
    }
}