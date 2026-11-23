-keep class com.verifyetb.app.data.local.entity.** { *; }
-keep class com.verifyetb.app.data.local.AppDatabase { *; }

-keepclassmembers class * extends androidx.room.RoomDatabase {
    androidx.room.InvalidationTracker getInvalidationTracker();
}
