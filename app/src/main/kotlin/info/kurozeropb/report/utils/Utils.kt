package info.kurozeropb.report.utils

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

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
}