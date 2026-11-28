# Fast Cash Sri Lanka - ProGuard / R8 Rules

# Keep Gson / Retrofit DTOs and data models for serialization
-keep class com.example.fastcash.data.remote.** { *; }
-keep class com.example.fastcash.data.model.** { *; }

# Keep generic signatures and annotations for Gson & Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations

# Suppress warnings for optional javax/sun references in Gson/OkHttp
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
