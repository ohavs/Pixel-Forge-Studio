# R8 rules
# Watch Face Push validator + APK signer: reflection/resources inside, keep whole.
-keep class com.google.android.wearable.watchface.validator.** { *; }
-keep class com.android.apksig.** { *; }
-dontwarn **
