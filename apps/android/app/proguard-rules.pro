-keep class com.t3tools.android.data.model.** { *; }
-keepclassmembers class com.t3tools.android.data.model.** { *; }

# ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel { <init>(...); }
-keep class androidx.lifecycle.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.t3tools.android.**$$serializer { *; }
-keepclassmembers class com.t3tools.android.** {
    *** Companion;
}
-keepclasseswithmembers class com.t3tools.android.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp + Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
