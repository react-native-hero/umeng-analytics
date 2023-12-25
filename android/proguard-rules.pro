-keep class com.umeng.** { *; }
-keep class com.uc.** { *; }
-keep class com.efs.** { *; }

-keepclassmembers class * {
   public <init> (org.json.JSONObject);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
