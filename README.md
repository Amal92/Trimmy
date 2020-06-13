# Usage

*Step 1:* Add the JitPack repository to your build file
Add it in your root build.gradle at the end of repositories:

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
  implementation 'com.github.Amal92:Trimmy:1.0.0'
}
```

# How to use

1. Add VideoTrimmerView in your layout.
```xml
 <com.amp.trimmy.VideoTrimmerView
        android:id="@+id/videoTrimmerView"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
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