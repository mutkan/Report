package info.kurozeropb.report

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.github.pwittchen.swipe.library.rx2.Swipe
import com.github.pwittchen.swipe.library.rx2.SwipeEvent
import info.kurozeropb.report.utils.Api.version
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_about.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import java.util.*

class AboutActivity : AppCompatActivity() {
    private lateinit var swipe: Swipe
    private lateinit var disposable: Disposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        setSupportActionBar(about_toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

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

        val year = Calendar.getInstance().get(Calendar.YEAR)
        tv_about.text = HtmlCompat.fromHtml("""
            <p>© $year — <a href="https://kurozeropb.info">Kurozero</a> | Build v$version<br/>
            Created using Kotlin</p>
        """.trimIndent(), HtmlCompat.FROM_HTML_MODE_LEGACY)
        tv_about.movementMethod = LinkMovementMethod.getInstance()

        btn_about_back.onClick { finish() }
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