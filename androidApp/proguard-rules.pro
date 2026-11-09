# kotlinx.serialization, Ktor, Koin, OkHttp and Coil ship their own consumer R8 rules.
# Add app-specific keep rules below only when a release build proves they're needed.

# Keep line numbers so release crash reports are readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Ktor references JVM classes that don't exist on Android.
-dontwarn java.lang.management.**
-dontwarn org.slf4j.**
