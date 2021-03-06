package info.kurozeropb.report.utils

import android.content.SharedPreferences
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.github.pwittchen.swipe.library.rx2.Swipe
import com.github.pwittchen.swipe.library.rx2.SwipeEvent
import com.google.android.material.snackbar.Snackbar
import info.kurozeropb.report.R
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.dimen
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

object Utils {
    lateinit var swipe: Swipe
    lateinit var disposable: Disposable
    lateinit var sharedPreferences: SharedPreferences

    private const val defaultSwipeThreshold = 350

    enum class SnackbarType {
        INFO,
        SUCCESS,
        ALERT,
        EXCEPTION
    }

    /** Tests if a string is valid JSON */
    fun isJSON(test: String): Boolean {
        try {
            JSONObject(test)
        } catch (ex: JSONException) {
            try {
                JSONArray(test)
            } catch (ex1: JSONException) {
                return false
            }
        }
        return true
    }

    /** Format ISO 8601 date string to [dd-MM-yyyy] */
    fun formatISOString(timeCreated: String): String {
        val parseDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
        val formatDate = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)
        val timeCreatedDate = parseDate.parse(timeCreated)
        return formatDate.format(timeCreatedDate)
    }

    fun showSnackbar(view: View, text: String, duration: Int, type: SnackbarType) {
        // Create snackbar
        val snackbar = Snackbar.make(view, text, duration)
        // Create snackbar layout
        val params = when (snackbar.view.layoutParams) {
            is FrameLayout.LayoutParams -> {
                snackbar.view.layoutParams as FrameLayout.LayoutParams
            }
            else -> {
                snackbar.view.layoutParams as CoordinatorLayout.LayoutParams
            }
        }

        // Get snackbar text and update the color
        val sbTextView = snackbar.view.findViewById<TextView>(R.id.snackbar_text)
        sbTextView.setTextColor(view.context.getColor(R.color.white))

        // Set the snackbar action and update the action text color
        snackbar.setAction("X") { snackbar.dismiss() }
        snackbar.setActionTextColor(view.context.getColor(R.color.white))

        // Update the snackbar layout
        params.setMargins(params.leftMargin + 10, params.topMargin, params.rightMargin + 10, params.bottomMargin + 10)
        snackbar.view.layoutParams = params

        // Sets snackbar background layout and returns corresponding icon drawable
        val sbIconDrawable = when (type) {
            SnackbarType.INFO -> {
                snackbar.view.background = view.context.getDrawable(R.drawable.sb_info_layout)
                view.context.getDrawable(R.drawable.sb_info_icon)
            }
            SnackbarType.SUCCESS -> {
                snackbar.view.background = view.context.getDrawable(R.drawable.sb_success_layout)
                view.context.getDrawable(R.drawable.sb_success_icon)
            }
            SnackbarType.ALERT -> {
                snackbar.view.background = view.context.getDrawable(R.drawable.sb_alert_layout)
                view.context.getDrawable(R.drawable.sb_alert_icon)
            }
            SnackbarType.EXCEPTION -> {
                snackbar.view.background = view.context.getDrawable(R.drawable.sb_exception_layout)
                view.context.getDrawable(R.drawable.sb_exception_icon)
            }
        }

        // Set the snackbar icon
        sbTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(sbIconDrawable, null, null, null)
        sbTextView.compoundDrawablePadding = view.context.dimen(R.dimen.snackbar_icon_padding)

        // Show snackbar
        snackbar.show()
    }

    fun createSwipe(): Observable<SwipeEvent> {
        swipe = Swipe(defaultSwipeThreshold, defaultSwipeThreshold)
        return swipe.observe()
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
    }
}