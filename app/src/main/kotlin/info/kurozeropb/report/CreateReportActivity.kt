package info.kurozeropb.report

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_create_report.*

class CreateReportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_report)
        setSupportActionBar(create_report_toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        btn_back.setOnClickListener {
            finish()
        }
    }
}