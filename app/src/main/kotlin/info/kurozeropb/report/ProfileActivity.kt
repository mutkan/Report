package info.kurozeropb.report

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.github.pwittchen.swipe.library.rx2.SwipeEvent
import kotlinx.android.synthetic.main.activity_profile.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import kotlinx.serialization.UnstableDefault
import android.app.Activity
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import java.io.File
import com.google.android.material.snackbar.Snackbar
import info.kurozeropb.report.structures.ImgUpload
import info.kurozeropb.report.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

        if (Api.isLoggedin && Api.user != null) {
            Glide.with(this)
                .load(Api.user!!.avatarUrl)
                .centerCrop()
                .apply(RequestOptions.circleCropTransform())
                .into(iv_avatar)
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
                    val selectedImageUri = intent!!.data
                    val imgPath = FilePickUtils.getSmartFilePath(this, selectedImageUri!!)
                    val file = File(imgPath)
                    postImage(file)
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

    /** Make iv_avatar round */
    private fun createRoundImageView() {
        val bitmap = iv_avatar.drawable.toBitmap()
        val circularBitmapDrawable = RoundedBitmapDrawableFactory.create(resources, bitmap)
        circularBitmapDrawable.isCircular = true
        iv_avatar.setImageDrawable(circularBitmapDrawable)
    }

    private fun postImage(file: File) {
        GlobalScope.launch(Dispatchers.IO) {
            val mediaType = "image/${file.extension}".toMediaTypeOrNull()
            val client = OkHttpClient()

            // Create multipart body (multipart was a bitch to figure out how it worked)
            val builder = MultipartBody.Builder()
            builder.setType(MultipartBody.FORM)
            builder.addFormDataPart("file", file.name, file.asRequestBody(mediaType))
            builder.addFormDataPart("key", Secrets.catgirlToken)
            builder.setType("multipart/form-data".toMediaTypeOrNull()!!)
            val requestBody = builder.build()

            // Set the headers
            val (versionName, versionCode) = Api.getVersions(this@ProfileActivity)
            val headers = Headers.Builder()
                .add("Authorization", Secrets.catgirlToken)
                .add("User-Agent", "ReportApp/$versionName ($versionCode) - https://github.com/reportapp/Report")
                .build()

            // Build the actual request
            val request = Request.Builder()
                .url("https://catgirlsare.sexy/api/upload") // catgirlsare.sexy is a private file host
                .headers(headers)
                .post(requestBody)
                .build()

            // Execute our request
            try {
                val response = client.newCall(request).execute()
                val data = Json.nonstrict.parse(ImgUpload.serializer(), response.body?.string() ?: "{\"success\": false}")
                if (data.success) {
                    withContext(Dispatchers.Main) {
                        if (Api.isLoggedin && Api.user != null) {
                            Glide.with(this@ProfileActivity)
                                .load(data.url)
                                .centerCrop()
                                .apply(RequestOptions.circleCropTransform())
                                .into(iv_avatar)
                        }
                    }

                    // Send request to api to update avatar url
                    if (data.url != null) {
                        val (message, error) = Api.updateAvatarAsync(data.url).await()
                        when {
                            message != null -> {
                                Utils.showSnackbar(profile_view, message, Snackbar.LENGTH_LONG, Utils.SnackbarType.SUCCESS)

                                // Update user info
                                val (user, userError) = Api.fetchUserInfoAsync().await()
                                when {
                                    user != null -> Api.user = user
                                    userError != null -> {
                                        Utils.showSnackbar(profile_view, userError.message, Snackbar.LENGTH_LONG, Utils.SnackbarType.EXCEPTION)
                                        return@launch
                                    }
                                    else -> {
                                        Utils.showSnackbar(profile_view, getString(R.string.failed_userinfo), Snackbar.LENGTH_LONG, Utils.SnackbarType.EXCEPTION)
                                        return@launch
                                    }
                                }

                                return@launch
                            }
                            error != null -> {
                                Utils.showSnackbar(profile_view, error.message, Snackbar.LENGTH_LONG, Utils.SnackbarType.EXCEPTION)
                                return@launch
                            }
                            else -> return@launch
                        }
                    }
                } else {
                    Utils.showSnackbar(profile_view, "Failed to upload image", Snackbar.LENGTH_LONG, Utils.SnackbarType.EXCEPTION)
                }
                response.close()
            } catch (e: IOException) {
                Utils.showSnackbar(profile_view, e.message ?: "Something went wrong", Snackbar.LENGTH_LONG, Utils.SnackbarType.EXCEPTION)
            }
        }
    }
}