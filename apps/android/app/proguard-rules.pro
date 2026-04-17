-keep class com.t3tools.android.data.model.** { *; }
-keepclassmembers class com.t3tools.android.data.model.** { *; }

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

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
