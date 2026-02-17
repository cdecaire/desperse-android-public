# proguard-rules.pro

# === APP-SPECIFIC ===

# Keep serializable DTOs and models (safety net for JSON deserialization)
-keep class app.desperse.data.dto.** { *; }
-keep class app.desperse.data.model.** { *; }

# === PRIVY SDK (ships minimal rules, needs protection) ===
-keep class io.privy.** { *; }
-dontwarn io.privy.**

# === MWA / SOLANA MOBILE (ships empty proguard.txt) ===
-keep class com.solana.mobilewalletadapter.** { *; }
-keep class com.solanamobile.** { *; }
-dontwarn com.solana.**

# === JNA & LAZYSODIUM (ship NO rules, use JNI/native) ===
-keep class com.sun.jna.** { *; }
-keep class net.java.dev.jna.** { *; }
-dontwarn com.sun.jna.**
-keep class com.goterl.lazysodium.** { *; }
-dontwarn com.goterl.lazysodium.**

# === COIL (ships NO consumer rules) ===
-dontwarn coil.**
