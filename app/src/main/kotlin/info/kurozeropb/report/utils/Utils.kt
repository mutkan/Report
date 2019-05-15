package info.kurozeropb.report.utils

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import info.kurozeropb.report.R
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

object Utils {
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

    fun showSnackbar(view: View, ctx: Context, text: String, duration: Int) {
        val snackbar = Snackbar.make(view, text, duration)
        val params = when (snackbar.view.layoutParams) {
            is FrameLayout.LayoutParams -> {
                snackbar.view.layoutParams as FrameLayout.LayoutParams
            }
            else -> {
                snackbar.view.layoutParams as CoordinatorLayout.LayoutParams
            }
        }

        val textViews: ArrayList<View> = arrayListOf()
        snackbar.view.findViewsWithText(textViews, text, View.FIND_VIEWS_WITH_TEXT)

        snackbar.setAction("X") { snackbar.dismiss() }
        params.setMargins(params.leftMargin + 10, params.topMargin, params.rightMargin + 10, params.bottomMargin + 10)
        snackbar.view.layoutParams = params
        snackbar.view.background = ctx.getDrawable(R.drawable.layout_bg)
        snackbar.show()
    }
}