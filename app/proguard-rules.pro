# Keep BuildConfig (used for DEBUG flag and API keys)
-keep class com.example.BuildConfig { *; }

# Keep data models used by Moshi (reflection-based JSON)
-keep class com.example.data.database.** { *; }
-keep class com.example.data.remote.FitSettingsDto { *; }
-keep class com.example.data.remote.RoutineDto { *; }
-keep class com.example.data.sync.SupabaseProfile { *; }
-keep class com.example.data.sync.SupabaseRoutine { *; }
-keep class com.example.data.sync.SupabasePlanExercise { *; }
-keep class com.example.data.sync.SupabaseSession { *; }
-keep class com.example.data.sync.SupabaseSessionLog { *; }
-keep class com.example.data.sync.SupabaseHeatmapData { *; }
-keep class com.example.data.api.AIService$MealAnalysisResult { *; }
-keep class com.example.data.api.AIService$FoodAnalysisResult { *; }

# Keep Kotlin Serialization classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.example.**$$serializer { *; }
-keepclassmembers class com.example.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Compose
-keep class androidx.compose.** { *; }

# Keep Room entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Moshi adapters
-keep class com.squareup.moshi.** { *; }
-keep class kotlin.reflect.jvm.internal.** { *; }

# OkHttp
-dontwarn okhttp3.internal.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn okhttp3.**

# Supabase/Ktor client
-dontwarn io.ktor.**
-dontwarn io.github.jan.supabase.**

# Remove debug logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
} 
# Keep error/warn logs for crash reporting
# public static int w(...);
# public static int e(...);
