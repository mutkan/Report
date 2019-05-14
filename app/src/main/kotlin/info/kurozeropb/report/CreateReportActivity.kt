package info.kurozeropb.report

import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.github.pwittchen.swipe.library.rx2.Swipe
import com.github.pwittchen.swipe.library.rx2.SwipeEvent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_create_report.*
import org.jetbrains.anko.sdk27.coroutines.onClick

class CreateReportActivity : AppCompatActivity() {
    private lateinit var swipe: Swipe
    private lateinit var disposable: Disposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_report)
        setSupportActionBar(create_report_toolbar)
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

        btn_back.onClick { finish() }
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