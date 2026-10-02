# R8 rules for the release build (minify and shrink are on).
# Most libraries bring their own rules; these are the ones from their documentation, written out so it is
# clear what the release build depends on. If a release build behaves differently from a debug build,
# the first thing to check is a class that is used only through its name (JSON, database, injection).

# ---------------------------------------------------------------------------
# Retrofit (the weather request) and OkHttp
# ---------------------------------------------------------------------------
# Retrofit reads the generic types and annotations of the API interface at run time.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault

# Keep the methods of interfaces that have Retrofit annotations (OpenMeteoApi).
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
# Keep the API interface itself, and the classes used in its suspend functions.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>
-keep,allowobfuscation,allowshrinking class retrofit2.Response

-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ---------------------------------------------------------------------------
# kotlinx.serialization (the JSON classes in data/remote)
# ---------------------------------------------------------------------------
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault

# Keep the Companion object of every @Serializable class ...
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
# ... and its serializer() function, for default and named companions.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
# ... and the same for serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn javax.servlet.**

# ---------------------------------------------------------------------------
# Room (the database). Room generates its code at build time; the database class is created by name.
# ---------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ---------------------------------------------------------------------------
# Hilt / Dagger (dependency injection) and WorkManager workers built by Hilt
# ---------------------------------------------------------------------------
# Hilt generates its classes at build time and ships its own consumer rules. The classes below are created
# by the system by name (from the manifest or by WorkManager), so they must keep their constructors.
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keep @dagger.hilt.android.lifecycle.HiltViewModel class *
-dontwarn dagger.hilt.internal.aggregatedroot.codegen.**

# ---------------------------------------------------------------------------
# Keep readable stack traces for crash reports
# ---------------------------------------------------------------------------
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
