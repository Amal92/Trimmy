package com.amp.video_trimmer_sample

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.amp.trimmy.interfaces.VideoTrimmingListener
import kotlinx.android.synthetic.main.activity_trimmer.trim_btn
import kotlinx.android.synthetic.main.activity_trimmer.trimmingProgressView
import kotlinx.android.synthetic.main.activity_trimmer.videoTrimmerView
import java.io.File

class TrimmerActivity : AppCompatActivity(), VideoTrimmingListener {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_trimmer)
    val inputVideoUri: Uri? = intent?.getParcelableExtra(MainActivity.EXTRA_INPUT_URI)
    if (inputVideoUri == null) {
      finish()
      return
    }

    videoTrimmerView.setMaxDurationInMs(10 * 1000)
    videoTrimmerView.setMinDurationInMs(1 * 1000)
    videoTrimmerView.setOnK4LVideoListener(this)
    val parentFolder = File(Environment.getExternalStorageDirectory(), "Video Trimmer Library")
    if (!parentFolder.exists()) {
      val flag = parentFolder.mkdirs()
    }
    val fileName = "trimmedVideo_${System.currentTimeMillis()}.mp4"
    val trimmedVideoFile = File(parentFolder, fileName)
    videoTrimmerView.setDestinationFile(trimmedVideoFile)
    videoTrimmerView.setVideoURI(inputVideoUri)
    videoTrimmerView.setVideoInformationVisibility(true)

    trim_btn.setOnClickListener {
      videoTrimmerView.initiateTrimming()
    }
  }

  override fun onTrimStarted() {
    trimmingProgressView.visibility = View.VISIBLE
  }

  override fun onFinishedTrimming(uri: Uri?) {
    runOnUiThread {
      trimmingProgressView.visibility = View.GONE
      if (uri == null) {
        Toast.makeText(this@TrimmerActivity, "failed trimming", Toast.LENGTH_SHORT)
            .show()
      } else {
        val msg = getString(R.string.video_saved_at, uri.path)
        Toast.makeText(this@TrimmerActivity, msg, Toast.LENGTH_SHORT)
            .show()
      }
      finish()
    }
  }

  override fun onTrimProgressing(progress: Double) {

  }

  override fun onErrorWhileViewingVideo(
    what: Int,
    extra: Int
  ) {
    trimmingProgressView.visibility = View.GONE
    Toast.makeText(this@TrimmerActivity, "error while previewing video", Toast.LENGTH_SHORT)
        .show()
  }

  override fun onTrimFailed(exception: Exception?) {

  }

  override fun onVideoPrepared() {

  }
}
