# ==============================================================================
# ZiroMaps ProGuard / R8 Hardening Rules
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. PdfBox-Android
# ------------------------------------------------------------------------------
-keep class com.tom_roush.pdfbox.** { *; }
-keep interface com.tom_roush.pdfbox.** { *; }
-keep class com.tom_roush.fontbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-dontwarn com.tom_roush.fontbox.**

# ------------------------------------------------------------------------------
# 2. AndroidX Room Database & SQLite
# ------------------------------------------------------------------------------
-keep class androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keepclassmembers class * {
    @androidx.room.Insert *;
    @androidx.room.Update *;
    @androidx.room.Delete *;
    @androidx.room.Query *;
}
-dontwarn androidx.room.paging.**

# ------------------------------------------------------------------------------
# 3. AndroidX CameraX & ExifInterface
# ------------------------------------------------------------------------------
-keep class androidx.camera.** { *; }
-keep class androidx.camera.core.** { *; }
-keep class androidx.camera.camera2.** { *; }
-keep class androidx.camera.lifecycle.** { *; }
-keep class androidx.camera.view.** { *; }
-dontwarn androidx.camera.**
-keep class androidx.exifinterface.media.ExifInterface { *; }

# ------------------------------------------------------------------------------
# 4. Domain Models & JSON Serialization / Export Entities
# ------------------------------------------------------------------------------
-keep class com.zirosaur.ziromaps.core.database.entity.** { *; }
-keep class com.zirosaur.ziromaps.core.database.model.** { *; }
-keep class com.zirosaur.ziromaps.core.geopdf.model.** { *; }
-keep class com.zirosaur.ziromaps.core.location.model.** { *; }
-keep class com.zirosaur.ziromaps.core.camera.model.** { *; }
-keep class com.zirosaur.ziromaps.core.export.model.** { *; }
-keep class com.zirosaur.ziromaps.model.** { *; }

# JSON classes and org.json
-keepclassmembers class * {
    public static ** fromJson(...);
    public java.lang.String toJson();
}

# ------------------------------------------------------------------------------
# 5. Dependency Injection (Hilt / Dagger) & AndroidX Lifecycle
# ------------------------------------------------------------------------------
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}
-dontwarn dagger.hilt.**
