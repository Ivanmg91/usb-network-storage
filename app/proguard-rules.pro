# smbj - keep SMB protocol classes
-keep class com.hierynomus.** { *; }
-keep class com.rapid7.** { *; }
-dontwarn com.hierynomus.**
-dontwarn org.bouncycastle.**
-dontwarn org.ietf.jgss.**

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.usblocal.app.**$$serializer { *; }
-keepclassmembers class com.usblocal.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.usblocal.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# SLF4J
-dontwarn org.slf4j.**
