# Aster Reader — release (R8) 规则
# debug 不开 minify，因此这些规则只在 release 生效；改动后务必跑一次 assembleRelease 验证。

# 崩溃堆栈可读
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses,EnclosingMethod

# Room 通过 Room.getGeneratedImplementation() 反射查找 <DatabaseClass>_Impl，
# 生成类与 DAO 实现必须保留，否则 release 启动即崩。
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class com.asterreader.core.database.** { *; }
-dontwarn androidx.room.paging.**
-dontwarn androidx.sqlite.db.**

# 归档解码库：commons-compress 的部分格式通过 ServiceLoader / 反射发现，
# 且 7z / RAR / XZ 的类在运行时才按格式选中，保持完整以免被摇掉。
-keep class org.apache.commons.compress.** { *; }
-keep class com.github.junrar.** { *; }
-keep class org.tukaani.xz.** { *; }
-dontwarn org.apache.commons.compress.**
-dontwarn com.github.junrar.**
-dontwarn org.slf4j.**

# Kotlin 协程的 volatile 字段用于取消/状态同步
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# Compose / Coil / Material3 的 consumer rules 由各自 AAR 提供，这里只兜底
-dontwarn androidx.compose.**
-dontwarn coil.**
