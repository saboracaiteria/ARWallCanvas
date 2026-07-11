# Regras ProGuard para ARWallCanvas
-keepattributes *Annotation*

# CameraX
-keep class androidx.camera.** { *; }

# DrawingEngine
-keep class com.arwallcanvas.drawing.** { *; }

# Material Components
-dontwarn com.google.android.material.**
-keep class com.google.android.material.** { *; }

# Kotlin
-dontwarn kotlin.**
-keep class kotlin.** { *; }
