# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Keep data classes for Room
-keep class com.rudra.smartworktracker.data.entity.** { *; }
-keep class com.rudra.smartworktracker.model.** { *; }

# Enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# R8 optimizations
-allowaccessmodification
-repackageclasses ''
-optimizationpasses 5

# Gson-persisted models (SharedPreferences team data, JSON backups). Field names are the
# storage format, so R8 must not rename them or stored data becomes unreadable after an update.
-keepattributes Signature, *Annotation*
-keep class com.rudra.smartworktracker.ui.screens.team.Team { *; }
-keep class com.rudra.smartworktracker.ui.screens.team.Teammate { *; }
-keep class com.rudra.smartworktracker.ui.screens.team.DutySchedule { *; }
-keep class com.rudra.smartworktracker.ui.screens.team.AssignedDuty { *; }
-keep class com.rudra.smartworktracker.ui.screens.team.Availability { *; }
-keep class com.rudra.smartworktracker.ui.screens.team.DutySwap { *; }
-keep class com.rudra.smartworktracker.ui.screens.team.SwapStatus { *; }
-keep class com.rudra.smartworktracker.data.backup.** { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.TypeAdapter
