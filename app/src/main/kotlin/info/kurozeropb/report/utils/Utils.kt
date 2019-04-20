package info.kurozeropb.report.utils

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class Utils {
    companion object {
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
    }
}