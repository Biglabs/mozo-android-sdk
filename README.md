# Mozo Android SDK
[ ![Download](https://api.bintray.com/packages/biglabs-vietnam/MozoSDK/mozo-sdk/images/download.svg) ](https://bintray.com/biglabs-vietnam/MozoSDK/mozo-sdk/_latestVersion)

MozoSDK for Android by Big Labs Pte. Ltd.
For more information please see [the website][1].

## Adding to project
Add this library as a dependency in your app's build.gradle file.
```
dependencies {
    implementation 'com.biglabs:mozo-sdk:0.0.1'
}
```
MozoSDK requires at minimum Java 8 and Android 5.0.

## Setting up
* First, in your module Gradle file (usually the `app/build.gradle`), add the `packagingOptions` and `compileOptions` to remove dupllicate files and make sure your project compiling by Java 8:
```
apply plugin: 'com.android.application'
android {
    // ...
    
    packagingOptions {
        exclude 'META-INF/rxjava.properties'
        exclude 'lib/x86_64/darwin/libscrypt.dylib'
        exclude 'lib/x86_64/freebsd/libscrypt.so'
        exclude 'lib/x86_64/linux/libscrypt.so'
    }
    
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
* [Transaction History][5]

## Sample
Build sample app with Gradle:
```
git clone https://github.com/Biglabs/mozo-android-sdk.git
cd mozo-android-sdk
./gradlew installDebug
```
**Note:** Make sure your Android SDK has the Android Support Repository installed, and that your `$ANDROID_HOME` environment variable is pointing at the SDK or add a `local.properties` file in the root project with a `sdk.dir=...` line.

## Give feedback

## License

[1]: https://mozocoin.io/
[2]: https://github.com/Biglabs/mozo-android-sdk/wiki/1.-UI-Components
[3]: https://github.com/Biglabs/mozo-android-sdk/wiki/2.-Authentication
[4]: https://github.com/Biglabs/mozo-android-sdk/wiki/3.-Send-Mozo
[5]: https://github.com/Biglabs/mozo-android-sdk/wiki/4.-Transaction-History