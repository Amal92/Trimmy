This code is built on top of [VideoTrimmer](https://github.com/AndroidDeveloperLB/VideoTrimmer). 

### Changes from original:
1. The UI for time selector info is modified.
2. The range selector is forced to begin from 00:00. In the original the range bar was positioned in the center.
3. The library used to trim the video is changed to [MP4Composer](https://github.com/MasayukiSuda/Mp4Composer-android) from [MP4Parser](https://github.com/sannies/mp4parser).
4. VideoTrimmerView is moved into the library for quick and easy setup.

# Set up

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
  implementation 'com.github.Amal92:Trimmy:1.0.1'
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
 videoTrimmerView.setOnK4LVideoListener(this)
 videoTrimmerView.setDestinationFile(trimmedVideoFile)
 videoTrimmerView.setVideoURI(inputVideoUri)
 videoTrimmerView.setVideoInformationVisibility(true)
 
 // use this method to start trimming the selected time frame
 videoTrimmerView.initiateTrimming()
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