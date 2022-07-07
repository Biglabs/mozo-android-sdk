# Mozo Android SDK
[![Maven Central](https://img.shields.io/maven-central/v/io.mozocoin/mozo-sdk.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.mozocoin%22%20AND%20a:%22mozo-sdk%22)
[ ![Android min version](https://img.shields.io/badge/Android-22+-brightgreen) ](#)
[ ![Kotlin version](https://img.shields.io/badge/Kotlin-1.7.0-blue) ](#)

MozoSDK for Android by MozoX Pte. Ltd.
For more information please see [the website][1].

## Adding to project
**MozoSDK requires at minimum Java 8 and Android 5.0 (API 21)**.
* Add MozoSDK repository to your root-level `build.gradle` file.
```
allprojects {
    repositories {
        // ...

        mavenCentral()
    }
}
```

* Add this library as a dependency in your `app/build.gradle` file.
<pre>
dependencies {
    implementation 'io.mozocoin:mozo-sdk:<a href="https://search.maven.org/search?q=mozo-sdk" title="Latest version">LATEST_VERSION</a>'
}
</pre>

## Setting up
* First, in your module Gradle file (usually the `app/build.gradle`), add the `compileOptions` to make sure your project compiling by Java 8:
```
apply plugin: 'com.android.application'
android {
    // ...
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}
```

* Then, initialize it in the `onCreate()` method:
```
MozoSDK.initialize(this)
```

## How do I use MozoSDK?

* [UI Components][2]
* [Authentication][3]
* [Send Mozo][4]
* [View Transaction History][5]

## Sample
Build sample app with Gradle:
```
git clone https://github.com/Biglabs/mozo-android-sdk.git
cd mozo-android-sdk
./gradlew installDebug
```
**Note:** Make sure your Android SDK has the Android Support Repository installed, and that your `$ANDROID_HOME` environment variable is pointing at the SDK or add a `local.properties` file in the root project with a `sdk.dir=...` line.

## Give feedback
To report a specific problem or feature request, [open a new issue on Github][6]. For questions, suggestions, or anything else, email to developer@mozocoin.io, or join our [Slack channel][7].

## License

[1]: https://mozocoin.io/
[2]: https://github.com/Biglabs/mozo-android-sdk/wiki/1.-UI-Components
[3]: https://github.com/Biglabs/mozo-android-sdk/wiki/2.-Authentication
[4]: https://github.com/Biglabs/mozo-android-sdk/wiki/3.-Send-Mozo
[5]: https://github.com/Biglabs/mozo-android-sdk/wiki/4.-Transaction-History
[6]: https://github.com/Biglabs/mozo-android-sdk/issues
[7]: https://mozocoin.slack.com
