[![](https://jitpack.io/v/Amal92/Trimmy.svg)](https://jitpack.io/#Amal92/Trimmy)


This code is built on top of [VideoTrimmer](https://github.com/AndroidDeveloperLB/VideoTrimmer). 

### Changes from original:
1. The UI for time selector info is modified.
2. The range selector is forced to begin from 00:00. In the original the range bar was positioned in the center.
3. The library used to trim the video is changed to [MP4Composer](https://github.com/MasayukiSuda/Mp4Composer-android) from [MP4Parser](https://github.com/sannies/mp4parser).
4. VideoTrimmerView is moved into the library for quick and easy setup.
5. Fixed TimeLineView not getting updated issue when a new video is loaded again.
6. Added watermark functionality.

# Gradle

*Step 1:* Add the JitPack repository to your root build file.

```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```
*Step 2:* Add the dependency:

```groovy
dependencies {
  implementation 'com.github.Amal92:Trimmy:<latest-version>'
}
```

# How to use

1. Add VideoTrimmerView in your layout.
```xml
 <com.amp.trimmy.VideoTrimmerView
        android:id="@+id/videoTrimmerView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
  />
```
2. Use the following methods to use VideoTrimmerView
```kotlin
 videoTrimmerView.setMaxDurationInMs(10 * 1000)
 videoTrimmerView.setMinDurationInMs(1 * 1000)
 videoTrimmerView.setOnK4LVideoListener(this)
 videoTrimmerView.setDestinationFile(trimmedVideoFile)

 // make sure the videoTimmerView is drawing before calling this method
 videoTrimmerView.post {
   videoTrimmerView.setVideoURI(inputVideoUri)
 }
 
 // Use this is control the visibility of time duration displayed
 videoTrimmerView.setVideoInformationVisibility(true)
 
 // Use this to set if the trimmed video should be mute
 videoTrimmerView.shouldMute(true) // Default is false
 
 // Use this to set the time scale of the trimmed video
 // Value should be in the range of -8(X) to 8(X)
 videoTrimmerView.setTimeScale(2) // Default is 1
 
 // Use this method to start trimming the selected time frame
 videoTrimmerView.initiateTrimming()

 // Use this method to draw watermark on the trimmed video
 videoTrimmerView.setWaterMark(BitmapFactory.decodeResource(resources, R.drawable.text_logo),WaterMarkPosition.RIGHT_BOTTOM,20,20)
```
3. Implement the listener to receive callback
```kotlin
class MainActivity : AppCompatActivity(), VideoTrimmingListener {

 override fun onErrorWhileViewingVideo(what: Int, extra: Int) {
    }

    override fun onFinishedTrimming(uri: Uri?) {
        // use this uri to the trimmed video file
    }

    override fun onTrimFailed(exception: Exception?) {
       
    }

    override fun onTrimProgressing(progress: Double) {
        
    }

    override fun onTrimStarted() {
       
    }

    override fun onVideoPrepared() {

    }

}
```