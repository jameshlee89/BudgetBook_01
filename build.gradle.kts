plugins {
    id("com.android.application") version "9.5.0" apply false
    // AGP 9 는 Kotlin 을 내장한다(kotlin-android 플러그인 불필요). Compose 컴파일러 플러그인만 추가한다.
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    id("com.google.gms.google-services") version "4.5.0" apply false
}
