/*
 * MIT License
 *
 * Copyright (c) 2016 Knowledge, education for life.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
@file:Suppress("LeakingThis")

package com.amp.trimmy

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Message
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import android.widget.VideoView
import androidx.annotation.UiThread
import com.amp.trimmy.interfaces.OnProgressVideoListener
import com.amp.trimmy.interfaces.OnRangeSeekBarListener
import com.amp.trimmy.interfaces.VideoTrimmingListener
import com.amp.trimmy.utils.BackgroundExecutor
import com.amp.trimmy.utils.UiThreadExecutor
import com.amp.trimmy.view.RangeSeekBarView
import com.amp.trimmy.view.TimeLineView
import com.daasuu.mp4compose.composer.Mp4Composer
import java.io.File
import java.lang.ref.WeakReference

abstract class BaseVideoTrimmerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val rangeSeekBarView: RangeSeekBarView
    private val videoViewContainer: View
    private val timeInfoContainer: View
    private val timeInfoContainer2: View
    private val videoView: VideoView
    private val playView: View
    private val timeLineView: TimeLineView
    private var src: Uri? = null
    private var dstFile: File? = null
    private var maxDurationInMs: Int = 0
    private var minDurationInMs: Int = 1000
    private var listeners = ArrayList<OnProgressVideoListener>()
    private var videoTrimmingListener: VideoTrimmingListener? = null
    private var duration = 0
    private var timeVideo = 0
    private var startPosition = 0
    private var endPosition = 0
    private var originSizeFile: Long = 0
    private var resetSeekBar = true
    private var timeScale: Int = 1
    private var shouldMute: Boolean = false
    private val messageHandler = MessageHandler(this)

    init {
        initRootView()
        rangeSeekBarView = getRangeSeekBarView()
        videoViewContainer = getVideoViewContainer()
        videoView = getVideoView()
        playView = getPlayView()
        timeInfoContainer = getTimeInfoContainer()
        timeInfoContainer2 = getTimeInfoContainer2()
        timeLineView = getTimeLineView()
        setUpListeners()
        setUpMargins()
    }

    abstract fun initRootView()

    abstract fun getTimeLineView(): TimeLineView

    abstract fun getTimeInfoContainer(): View

    abstract fun getTimeInfoContainer2(): View

    abstract fun getPlayView(): View

    abstract fun getVideoView(): VideoView

    abstract fun getVideoViewContainer(): View

    abstract fun getRangeSeekBarView(): RangeSeekBarView

    abstract fun onRangeUpdated(
        startTimeInMs: Int,
        endTimeInMs: Int
    )

    /**occurs during playback, to tell that you've reached a specific time in the video*/
    abstract fun onVideoPlaybackReachingTime(timeInMs: Int)

    abstract fun onGotVideoFileSize(videoFileSize: Long)

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpListeners() {
        listeners.add(object : OnProgressVideoListener {
            override fun updateProgress(
                time: Int,
                max: Int,
                scale: Float
            ) {
                this@BaseVideoTrimmerView.updateVideoProgress(time)
            }
        })
        val gestureDetector = GestureDetector(context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    onClickVideoPlayPause()
                    return true
                }
            }
        )
        videoView.setOnErrorListener { _, what, extra ->
            if (videoTrimmingListener != null)
                videoTrimmingListener!!.onErrorWhileViewingVideo(what, extra)
            false
        }

        videoView.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        rangeSeekBarView.addOnRangeSeekBarListener(object : OnRangeSeekBarListener {
            override fun onCreate(
                rangeSeekBarView: RangeSeekBarView,
                index: Int,
                value: Float
            ) {
                // Do nothing
            }

            override fun onSeek(
                rangeSeekBarView: RangeSeekBarView,
                index: Int,
                value: Float
            ) {
                onSeekThumbs(index, value)
            }

            override fun onSeekStart(
                rangeSeekBarView: RangeSeekBarView,
                index: Int,
                value: Float
            ) {
                // Do nothing
            }

            override fun onSeekStop(
                rangeSeekBarView: RangeSeekBarView,
                index: Int,
                value: Float
            ) {
                onStopSeekThumbs()
            }
        })
        videoView.setOnPreparedListener { this.onVideoPrepared(it) }
        videoView.setOnCompletionListener { onVideoCompleted() }
    }

    private fun setUpMargins() {
        val marge = rangeSeekBarView.thumbWidth
        val lp: MarginLayoutParams = timeLineView.layoutParams as MarginLayoutParams
        lp.setMargins(marge, lp.topMargin, marge, lp.bottomMargin)
        timeLineView.layoutParams = lp
    }

    @Suppress("unused")
    @UiThread
    fun initiateTrimming() {
        pauseVideo()
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(context, src)
        val metadataKeyDuration =
            java.lang.Long.parseLong(
                mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            )
        if (timeVideo < minDurationInMs) {
            /* if (metadataKeyDuration - endPosition > MIN_TIME_FRAME - timeVideo) {
                 endPosition += MIN_TIME_FRAME - timeVideo
             } else if (startPosition > MIN_TIME_FRAME - timeVideo) {
                 startPosition -= MIN_TIME_FRAME - timeVideo
             }*/
            val sec = if (minDurationInMs / 1000 == 1) "second" else "seconds"
            Toast.makeText(context, "Duration must be atleast ${minDurationInMs / 1000} $sec.", Toast.LENGTH_SHORT)
                .show()
            return
        }

        //notify that video trimming started
        if (videoTrimmingListener != null)
            videoTrimmingListener!!.onTrimStarted()

        Mp4Composer(getPath(src)!!, dstFile!!.path)
            .trim(startPosition.toLong(), endPosition.toLong())
            .mute(shouldMute)
            .timeScale(timeScale)
            .listener(object : Mp4Composer.Listener {
                override fun onFailed(exception: Exception?) {
                    videoTrimmingListener!!.onTrimFailed(exception)
                }

                override fun onProgress(progress: Double) {
                    videoTrimmingListener!!.onTrimProgressing(progress)
                }

                override fun onCanceled() {

                }

                override fun onCompleted() {
                    videoTrimmingListener!!.onFinishedTrimming(Uri.fromFile(dstFile))
                }

            })
            .start()
        /*BackgroundExecutor.execute(
            object : BackgroundExecutor.Task(null, 0L, null) {
                override fun execute() {
                    try {
                        TrimVideoUtils.startTrim(
                            context,
                            src!!,
                            dstFile!!,
                            startPosition.toLong(),
                            endPosition.toLong(),
                            duration.toLong(),
                            videoTrimmingListener!!
                        )
                    } catch (e: Throwable) {
                        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e)
                    }
                }
            }
        )*/

    }

    private fun getPath(uri: Uri?): String? {
        if (uri == null)
            return null
        val projection = arrayOf(MediaStore.Video.Media.DATA)
        val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, null)
        return if (cursor != null) {
            val column_index: Int = cursor
                .getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            cursor.moveToFirst()
            val filePath = cursor.getString(column_index)
            cursor.close()
            filePath
        } else null
    }

    private fun onClickVideoPlayPause() {
        if (videoView.isPlaying) {
            messageHandler.removeMessages(SHOW_PROGRESS)
            pauseVideo()
        } else {
            playView.visibility = View.GONE
            if (resetSeekBar) {
                resetSeekBar = false
                videoView.seekTo(startPosition)
            }
            messageHandler.sendEmptyMessage(SHOW_PROGRESS)
            videoView.start()
        }
    }

    @UiThread
    private fun onVideoPrepared(mp: MediaPlayer) {
        // Adjust the size of the video
        // so it fits on the screen
        val videoWidth = mp.videoWidth
        val videoHeight = mp.videoHeight
        val videoProportion = videoWidth.toFloat() / videoHeight.toFloat()
        val screenWidth = videoViewContainer.width
        val screenHeight = videoViewContainer.height
        val screenProportion = screenWidth.toFloat() / screenHeight.toFloat()
        val lp = videoView.layoutParams
        if (videoProportion > screenProportion) {
            lp.width = screenWidth
            lp.height = (screenWidth.toFloat() / videoProportion).toInt()
        } else {
            lp.width = (videoProportion * screenHeight.toFloat()).toInt()
            lp.height = screenHeight
        }
        videoView.layoutParams = lp
        playView.visibility = View.VISIBLE
        duration = videoView.duration
        setSeekBarPosition()
        onRangeUpdated(startPosition, endPosition)
        onVideoPlaybackReachingTime(0)
        if (videoTrimmingListener != null)
            videoTrimmingListener!!.onVideoPrepared()
    }

    private fun setSeekBarPosition() {
        if (duration >= maxDurationInMs) {
            startPosition = 0
            endPosition = maxDurationInMs
            rangeSeekBarView.setThumbValue(0, startPosition * 100f / duration)
            rangeSeekBarView.setThumbValue(1, endPosition * 100f / duration)
        } else {
            startPosition = 0
            endPosition = duration
        }
        setProgressBarPosition(startPosition)
        videoView.seekTo(startPosition)
        timeVideo = duration
        rangeSeekBarView.initMaxWidth()
    }

    private fun onSeekThumbs(
        index: Int,
        value: Float
    ) {
        when (index) {
            RangeSeekBarView.ThumbType.LEFT.index -> {
                startPosition = (duration * value / 100L).toInt()
                videoView.seekTo(startPosition)
            }
            RangeSeekBarView.ThumbType.RIGHT.index -> {
                endPosition = (duration * value / 100L).toInt()
            }
        }
        setProgressBarPosition(startPosition)

        onRangeUpdated(startPosition, endPosition)
        timeVideo = endPosition - startPosition
    }

    private fun onStopSeekThumbs() {
        messageHandler.removeMessages(SHOW_PROGRESS)
        pauseVideo()
    }

    private fun onVideoCompleted() {
        videoView.seekTo(startPosition)
    }

    private fun notifyProgressUpdate(all: Boolean) {
        if (duration == 0) return
        val position = videoView.currentPosition
        if (all)
            for (item in listeners)
                item.updateProgress(position, duration, position * 100f / duration)
        else
            listeners[1].updateProgress(position, duration, position * 100f / duration)
    }

    private fun updateVideoProgress(time: Int) {
        if (time >= endPosition) {
            messageHandler.removeMessages(SHOW_PROGRESS)
            pauseVideo()
            resetSeekBar = true
            return
        }
        setProgressBarPosition(time)
        onVideoPlaybackReachingTime(time)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun pauseVideo() {
        videoView.pause()
        playView.visibility = View.VISIBLE
    }

    private fun setProgressBarPosition(position: Int) {
        if (duration > 0) {
        }
    }

    /**
     * Set the time scale of the trimmed video.
     * @param timeScale should be in the range of -8 to 8, default = 1
     */
    fun setTimeScale(timeScale: Int) {
        this.timeScale = timeScale
    }

    /**
     * Set if the trimmed video should be muted.
     *
     * @param shouldMute true/false, default = false
     */
    fun shouldMute(shouldMute: Boolean) {
        this.shouldMute = shouldMute
    }

    /**
     * Set video information visibility.
     * For now this is for debugging
     *
     * @param visible whether or not the videoInformation will be visible
     */
    fun setVideoInformationVisibility(visible: Boolean) {
        timeInfoContainer.visibility = if (visible) View.VISIBLE else View.GONE
        timeInfoContainer2.visibility = if (visible) View.VISIBLE else View.GONE
    }

    /**
     * Listener for some [VideoView] events
     *
     * @param onK4LVideoListener interface for events
     */
    fun setOnK4LVideoListener(onK4LVideoListener: VideoTrimmingListener) {
        this.videoTrimmingListener = onK4LVideoListener
    }

    fun setDestinationFile(dst: File) {
        this.dstFile = dst
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        //Cancel all current operations
        BackgroundExecutor.cancelAll("", true)
        UiThreadExecutor.cancelAll("")
    }

    /**
     * Set the maximum duration of the trimmed video.
     * The trimmer interface wont allow the user to set duration longer than maxDuration
     */
    fun setMaxDurationInMs(maxDurationInMs: Int) {
        this.maxDurationInMs = maxDurationInMs
    }

    /**
     * Set the minimum duration of the trimmed video.
     * The trimmer interface wont allow the user to set duration longer than minDuration
     */
    fun setMinDurationInMs(minDurationInMs: Int) {
        this.minDurationInMs = minDurationInMs
    }

    /**
     * Sets the uri of the video to be trimmer
     *
     * @param videoURI Uri of the video
     */
    fun setVideoURI(videoURI: Uri) {
        src = videoURI
        if (originSizeFile == 0L) {
            val cursor = context.contentResolver.query(videoURI, null, null, null, null)
            if (cursor != null) {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                cursor.moveToFirst()
                originSizeFile = cursor.getLong(sizeIndex)
                cursor.close()
                onGotVideoFileSize(originSizeFile)
            }
        }
        videoView.setVideoURI(src)
        videoView.requestFocus()
        timeLineView.setVideo(src!!)
    }

    private class MessageHandler internal constructor(view: BaseVideoTrimmerView) : Handler() {
        private val mView: WeakReference<BaseVideoTrimmerView> = WeakReference(view)

        override fun handleMessage(msg: Message?) {
            val view = mView.get()
            if (view?.videoView == null)
                return
            view.notifyProgressUpdate(true)
            if (view.videoView.isPlaying) {
                sendEmptyMessageDelayed(0, 10)
            }
        }
    }

    companion object {
        private const val MIN_TIME_FRAME = 1000
        private const val SHOW_PROGRESS = 2
    }
}
