package com.github.reactnativehero.umenganalytics

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import android.webkit.WebSettings
import androidx.core.app.ActivityCompat
import com.facebook.react.bridge.*
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure
import com.umeng.commonsdk.statistics.common.DeviceConfig


class RNTUmengAnalyticsModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), LifecycleEventListener {

    companion object {

        private var appKey = ""
        private var pushSecret = ""
        private var channel = ""

        // 初始化友盟基础库
        @JvmStatic fun init(app: Application, metaData: Bundle, debug: Boolean) {

            appKey = metaData.getString("UMENG_APP_KEY", "").trim()
            pushSecret = metaData.getString("UMENG_PUSH_SECRET", "").trim()
            channel = metaData.getString("UMENG_CHANNEL", "").trim()

            UMConfigure.setLogEnabled(debug)
            UMConfigure.preInit(app, appKey, channel)

        }

    }

    init {
        reactContext.addLifecycleEventListener(this)
    }

    override fun getName(): String {
        return "RNTUmengAnalytics"
    }

    override fun getConstants(): Map<String, Any>? {

        val constants: MutableMap<String, Any> = HashMap()

        constants["CHANNEL"] = channel

        return constants

    }

    @ReactMethod
    fun init() {

        UMConfigure.init(reactContext, appKey, channel, UMConfigure.DEVICE_TYPE_PHONE, pushSecret)

        // 手动采集
        MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.MANUAL)

    }

    @ReactMethod
    fun getDeviceInfo(promise: Promise) {

        val deviceId = DeviceConfig.getDeviceIdForGeneral(reactContext)
        val deviceType = DeviceConfig.getDeviceType(reactContext)
        val brand = Build.BRAND
        val bundleId = DeviceConfig.getPackageName(reactContext)

        val map = Arguments.createMap()
        map.putString("deviceId", deviceId.toLowerCase())
        map.putString("deviceType", deviceType.toLowerCase())
        map.putString("brand", brand.toLowerCase())
        map.putString("bundleId", bundleId)

        promise.resolve(map)

    }

    @ReactMethod
    fun getUserAgent(promise: Promise) {

        val map = Arguments.createMap()

        try {
            val userAgent = WebSettings.getDefaultUserAgent(reactContext)
            map.putString("userAgent", userAgent)
        } catch (e: RuntimeException) {
            val userAgent = System.getProperty("http.agent")
            if (userAgent != null && userAgent.isNotEmpty()) {
                map.putString("userAgent", userAgent)
            }
            else {
                map.putString("error", e.localizedMessage)
            }
        }

        promise.resolve(map)

    }

    @ReactMethod
    fun getPhoneNumber(promise: Promise) {

        var hasPermission = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val readPhoneStatePermission: Int = ActivityCompat.checkSelfPermission(reactContext, Manifest.permission.READ_PHONE_STATE)
            if (readPhoneStatePermission != PackageManager.PERMISSION_GRANTED) {
                hasPermission = false
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val readPhoneNumbersPermission: Int = ActivityCompat.checkSelfPermission(reactContext, Manifest.permission.READ_PHONE_NUMBERS)
            if (readPhoneNumbersPermission != PackageManager.PERMISSION_GRANTED) {
                hasPermission = false
            }
        }

        val map = Arguments.createMap()

        if (hasPermission) {
            try {
                val manager = reactContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                map.putString("phoneNumber", manager.line1Number)
            }
            catch (e: Exception) {
                map.putString("error", e.localizedMessage)
            }
        }
        else {
            map.putString("error", "no permission")
        }

        promise.resolve(map)

    }

    @ReactMethod
    fun signIn(name: String, provider: String?) {
        val hasProvider = provider?.isNotEmpty() ?: false
        if (hasProvider) {
            MobclickAgent.onProfileSignIn(provider, name)
        }
        else {
            MobclickAgent.onProfileSignIn(name)
        }
    }

    @ReactMethod
    fun signOut() {
        MobclickAgent.onProfileSignOff()
    }

    @ReactMethod
    fun exitApp() {
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    @ReactMethod
    fun enterPage(pageName: String) {
        MobclickAgent.onPageStart(pageName)
    }

    @ReactMethod
    fun leavePage(pageName: String) {
        MobclickAgent.onPageEnd(pageName)
    }

    @ReactMethod
    fun sendEvent(eventId: String) {
        MobclickAgent.onEvent(reactContext, eventId)
    }

    @ReactMethod
    fun sendEventLabel(eventId: String, label: String) {
        MobclickAgent.onEvent(reactContext, eventId, label)
    }

    @ReactMethod
    fun sendEventData(eventId: String, data: ReadableMap) {
        val map = data.toHashMap()
        MobclickAgent.onEventObject(reactContext, eventId, map)
    }

    @ReactMethod
    fun sendEventCounter(eventId: String, data: ReadableMap, counter: Int) {
        val map = HashMap<String, String>()
        for ((key, value) in data.toHashMap()) {
            map[key] = value as String
        }
        MobclickAgent.onEventValue(reactContext, eventId, map, counter)
    }

    @ReactMethod
    fun sendError(error: String) {
        MobclickAgent.reportError(reactContext, error)
    }

    override fun onHostResume() {
        MobclickAgent.onResume(reactContext.currentActivity)
    }

    override fun onHostPause() {
        MobclickAgent.onPause(reactContext.currentActivity)
    }

    override fun onHostDestroy() {

    }

}
