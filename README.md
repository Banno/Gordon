# Gordon

![Latest release](https://img.shields.io/github/v/release/Banno/Gordon)

Gordon is an Android instrumentation test runner designed for speed, simplicity, and reliability. We built it because neither [Spoon](https://github.com/square/spoon) nor [Fork](https://github.com/shazam/fork) were fast enough nor reliable enough for us, and in attempts to fork those libraries we found them to be too old and complicated to be worth modifying. So we wrote Gordon from the ground up, using modern Gradle functionality and Kotlin coroutines.

#### Key features
- Several [pooling strategies](#pooling-strategies) (similar to what is offered by Fork)
- Smart [retries](#retries) (if [configured](#configuring))
- Flexible test filtering, configurable [from Gradle](#configuring) or [commandline](#filtering)
- JUnit and HTML [reports](#reports)

#### See also:
[Better Android Instrumentation Testing with Gordon](https://proandroiddev.com/gordon-android-testing-49a5448317aa) on ProAndroidDev

## Setup

### With Gradle plugins block

#### settings.gradle.kts of your root project
```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        jcenter()
        maven("https://www.jitpack.io")
    }

    plugins {
        id("com.banno.gordon") version "$gordonVersion"
    }
}
```

#### build.gradle.kts of any modules for which you want to run tests using Gordon
```kotlin
plugins {
    id("com.banno.gordon")
}
```

### With old Gradle plugins syntax

#### build.gradle of your root project
```groovy
buildscript {
    repositories {
        gradlePluginPortal()
        google()
        jcenter()
        maven { url "https://jitpack.io" }
    }

    dependencies {
        classpath "com.banno.gordon:gordon-plugin:$gordonVersion"
    }
}
```

#### build.gradle of any modules for which you want to run tests using Gordon
```groovy
apply plugin: "com.banno.gordon"
```

### buildSrc

If you have a buildSrc module, you may also need to add the dependency there if you get aapt2 errors.

#### build.gradle.kts of your buildSrc module
```kotlin
repositories {
    gradlePluginPortal()
    google()
    jcenter()
    maven("https://www.jitpack.io")
}

dependencies {
    implementation("com.banno.gordon:gordon-plugin:$gordonVersion")
}
```

### Configuring

#### build.gradle.kts of any module for which you've applied Gordon
```kotlin
import com.banno.gordon.PoolingStrategy

gordon {
    // Default is PoolingStrategy.PoolPerDevice
    poolingStrategy.set(PoolingStrategy.PhonesAndTablets)
    //or
    poolingStrategy.set(
        PoolingStrategy.Manual(
            mapOf(
                "poolOne" to setOf("deviceSerial1", "deviceSerial2"),
                "poolTwo" to setOf("deviceSerial3", "deviceSerial4")
            )
        )
    )

    // Default is unset (`-1`) - to use "tablet" characteristic instead of size
    tabletShortestWidthDp(720)

    // Default is 0
    retryQuota.set(2)

    // Default is 120_000 (2 minutes)
    installTimeoutMillis.set(180_000)

    // Default is 120_000 (2 minutes)
    testTimeoutMillis.set(60_000)

    // Default is no filter
    testFilter.set("ExampleTest.runThisMethod,RunThisWholeTestClass,com.example.runthispackage,com.example.RunTestsWithThisAnnotation")
}
```

##### Groovy build.gradle example for `PoolingStrategy`
```groovy
import com.banno.gordon.PoolingStrategy

gordon {
    poolingStrategy.set(PoolingStrategy.PhonesAndTablets.INSTANCE)
    //or
    poolingStrategy.set(
            new PoolingStrategy.Manual(
                    [
                            "poolOne": ["deviceSerial1", "deviceSerial2"].toSet(),
                            "poolTwo": ["deviceSerial3", "deviceSerial4"].toSet()
                    ]
            )
    )
}
```

#### Pooling strategies
- `PoolPerDevice` - each device is its own pool, so each test will run on each device
- `SinglePool` - all devices make up one pool, so each test will run only once, on an unspecified device
- `PhonesAndTablets` - devices are split into pools based on type, so each test will run on one phone and one tablet
  - If the `tabletShortestWidthDp` property is set, devices with at least that dimension will be considered "tablets"
  - If `tabletShortestWidthDp` is not set, devices with `tablet` in their `ro.build.characteristics` build property will be considered "tablets"
- `Manual` - create your own pools with specific devices - each test will run on one device from each pool

### Compatibility with Android extension
Gordon is compatible with most testing options that can be configured in the Android extension, including `size`/`annotation`/`notAnnotation` arguments and disabling animations for tests. Note that you can also specify annotations using Gordon's test filtering options instead of using instrumentation runner arguments.

#### Example build.gradle.kts
```kotlin
android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        testInstrumentationRunnerArgument("size", "medium")
        testInstrumentationRunnerArgument("notAnnotation", "androidx.test.filters.FlakyTest")

        testOptions.animationsDisabled = true
    }
}
```

In this example, the AndroidX `AndroidJUnitRunner` will be used, animations will be disabled, and the only tests run will be those annotated with `@MediumTest`, but not `@FlakyTest`.

## Running

#### Tasks
Gordon registers a Gradle task for each tested variant, stripping `Debug` from the task name because it's redundant.

For example, if you have no flavors defined, the following task is registered:
- `gordon` - the equivalent of `connectedDebugAndroidTest`

If you have a `mode` dimension with `demo` and `full` flavors, plus a `staging` build type in addition to the standard `debug` and `release` types, the following tasks are registered:
- `gordonDemo` - the equivalent of `connectedDemoDebugAndroidTest`
- `gordonFull` - the equivalent of `connectedFullDebugAndroidTest`
- `gordonDemoStaging` - the equivalent of `connectedDemoStagingAndroidTest`
- `gordonFullStaging` - the equivalent of `connectedFullStagingAndroidTest`

#### Filtering
There is a `--tests` commandline option that overrides the `testFilter` set in the `gordon` extension if both are specified.

#### Examples
- `./gradlew gordon`
- `./gradlew gordon --tests=ExampleTest.runThisMethod`
- `./gradlew gordon --tests=RunThisWholeTestClass`
- `./gradlew gordon --tests=ExampleTest.runThisMethod,com.example.runthispackage`
- `./gradlew gordon --tests=com.example.RunTestsWithThisAnnotation`

#### Retries
If a retry quota is specified, Gordon will, after trying tests once, first retry any tests that were not able to run because of device issues, up to the specified quota per test case, and then retry any failing tests, up to the specified quota per test case. If multiple devices are available in a pool, a failing test will be retried on a different device from the one on which it originally failed.

#### Reports
Gordon generates junit reports in the build directory / `test-results`, and an HTML report in the build directory / `reports`.

## Other notes
- Gordon does not support [ABI/density splits](https://developer.android.com/studio/build/configure-apk-splits). We highly recommend you use [the App Bundle format](https://developer.android.com/guide/app-bundle) instead. However, Gordon v1.3.1 and older versions support splits, so you may be able to use an old version for now.

## Contributing

Contributions are welcome. You can use the included `app` module to locally test changes to the Gordon plugin.
1. Make changes to Gordon plugin
2. Run `./gradlew publishToMavenLocal`
3. Uncomment [com.banno.gordon in app plugins](https://github.com/Banno/Gordon/blob/master/app/build.gradle.kts#L6) and change the version to the one you just deployed
4. Test your changes by running `./gradlew gordon` to run `app` tests using the locally-deployed Gordon

## Why we named our test runner Gordon
![Gordon](https://user-images.githubusercontent.com/12698923/66937311-dbd1b580-f004-11e9-8faf-6dd2c7074485.png)

## License
```
   Copyright 2019 Jack Henry & Associates, Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```
