package info.kurozeropb.report

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.github.pwittchen.swipe.library.rx2.SwipeEvent
import info.kurozeropb.report.utils.Utils
import kotlinx.android.synthetic.main.activity_profile.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import info.kurozeropb.report.utils.Api
import info.kurozeropb.report.utils.LocaleHelper
import kotlinx.serialization.UnstableDefault
import android.app.Activity
import android.net.Uri
import android.util.Log
import java.io.File
import android.provider.MediaStore
import com.google.android.material.snackbar.Snackbar
import info.kurozeropb.report.structures.ImgurData
import info.kurozeropb.report.utils.Secrets
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.IOException


@UnstableDefault
class ProfileActivity : AppCompatActivity() {
    private val REQUEST_GET_SINGLE_FILE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        setSupportActionBar(profile_toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        createRoundImageView()

        tv_card_fullname.text = if (Api.user != null) "${Api.user?.firstName ?: "<first_name>"} ${Api.user?.lastName ?: "<last_name>"}" else "<full_name>"

        val observable = Utils.createSwipe()
        Utils.disposable = observable.subscribe { swipeEvent ->
            when (swipeEvent) {
                SwipeEvent.SWIPING_RIGHT -> finish() // Return to main activity when swiped to the right
                else -> return@subscribe
            }
        }

        btn_profile_back.onClick { finish() }

        iv_avatar.onClick {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_GET_SINGLE_FILE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        try {
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == REQUEST_GET_SINGLE_FILE) {
                    // TODO: Save image externally and put external url in database

                    var selectedImageUri = intent!!.data
                    // Get the path from the Uri
                    val path = getPathFromURI(selectedImageUri)
                    if (path != null) {
                        val f = File(path)
                        selectedImageUri = Uri.fromFile(f)
                    }
                    // Set the image in ImageView
                    iv_avatar.setImageURI(selectedImageUri)
                    createRoundImageView()

                    GlobalScope.async {
                        val file = File(path)
                        val mediaType = "image/${file.extension}".toMediaTypeOrNull()
                        val client = OkHttpClient()

                        Log.i("EXTENSION", file.extension)

                        // Create multipart body (multipart was a bitch to figure out how it worked)
                        val builder = MultipartBody.Builder()
                        builder.setType(MultipartBody.FORM)
                        builder.addFormDataPart("key", Secrets.catgirlToken)
                        builder.addFormDataPart("file", file.name, file.asRequestBody(mediaType))
                        builder.setType("multipart/form-data".toMediaTypeOrNull()!!)
                        val requestBody = builder.build()

                        // Set the headers
                        val headers = Headers.Builder()
                            // .add("Authorization", "Client-ID ${Secrets.imgurClient}")
                            .add("Authorization", Secrets.catgirlToken)
                            .build()

                        // Build the actual request
                        val request = Request.Builder()
                            // .url("https://api.imgur.com/3/image")
                            .url("https://catgirlsare.sexy/api/upload")
                            .headers(headers)
                            .post(requestBody)
                            .build()

                        try {
                            val response = client.newCall(request).execute()
                            if (response.isSuccessful) {
                                print(response.body!!.string())
                                // val res = Json.nonstrict.parse(ImgurData.serializer(), response.body!!.string())
                                // print(res)
                                // Log.i("LINK", res.link)
                            } else {
                                print(response.message)
                                Utils.showSnackbar(profile_view, "Failed to upload image", Snackbar.LENGTH_LONG, Utils.SnackbarType.EXCEPTION)
                            }
                            response.close()
                        } catch (e: IOException) {
                            Utils.showSnackbar(profile_view, e.message ?: "Something went wrong", Snackbar.LENGTH_LONG, Utils.SnackbarType.EXCEPTION)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FileSelectorActivity", "File select error", e)
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

    /** Get image path from uri */
    private fun getPathFromURI(contentUri: Uri?): String? {
        var res: String? = null
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        if (contentUri != null) {
            val cursor = contentResolver.query(contentUri, proj, null, null, null)
            if (cursor!!.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                res = cursor.getString(columnIndex)
            }
            cursor.close()
            return res
        }
        return null
    }

    /** Make iv_avatar round */
    private fun createRoundImageView() {
        val bitmap = iv_avatar.drawable.toBitmap()
        val circularBitmapDrawable = RoundedBitmapDrawableFactory.create(resources, bitmap)
        circularBitmapDrawable.isCircular = true
        iv_avatar.setImageDrawable(circularBitmapDrawable)
    }
}