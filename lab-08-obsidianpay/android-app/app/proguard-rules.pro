# ObsidianPay (Lab 08) — ProGuard/R8 rules.
# Release builds in this lab do not enable minification, so these rules are a
# minimal, safe baseline. A future phase (binary patching / RE) may revisit this.

# Keep app model classes (parsed via org.json by name in this phase).
-keep class com.obsidianpay.mobile.api.** { *; }

# OkHttp ships its own consumer rules; nothing extra needed here.
-dontwarn okhttp3.**
-dontwarn okio.**
