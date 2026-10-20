# Proguard / R8 rules for Universal TV Remote

# Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt

# Bouncy Castle (X.509 Certificate Generation & TLS)
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Android Architecture & Jetpack Compose
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ConsumerIrManager and hardware services
-keepclassmembers class * {
    android.hardware.ConsumerIrManager *;
}
