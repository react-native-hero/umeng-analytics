package com.github.reactnativehero.umenganalytics

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings.Secure
import android.telephony.TelephonyManager
import android.webkit.WebSettings
import androidx.core.app.ActivityCompat
import com.facebook.react.bridge.*
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure
import com.umeng.commonsdk.statistics.common.DeviceConfig
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap
import androidx.core.content.edit


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

    private var isReady = false

    override fun getName(): String {
        return "RNTUmengAnalytics"
    }

    override fun getConstants(): Map<String, Any> {

        val constants: MutableMap<String, Any> = HashMap()

        constants["CHANNEL"] = channel

        return constants

    }

    @ReactMethod
    fun init(promise: Promise) {

        UMConfigure.submitPolicyGrantResult(reactContext, true)
        UMConfigure.init(reactContext, appKey, channel, UMConfigure.DEVICE_TYPE_PHONE, pushSecret)

        // 手动采集
        MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.MANUAL)

        isReady = true

        promise.resolve(Arguments.createMap())

    }

    @ReactMethod
    fun getDeviceInfo(promise: Promise) {

        if (!checkReady(promise)) {
            return
        }

        fun tryDeviceInfo() {
            val map = this.getDeviceInfoMap()
            if (map.getString("deviceId").isNullOrEmpty()) {
                Executors.newSingleThreadScheduledExecutor().schedule({
                    tryDeviceInfo()
                }, 50, TimeUnit.MILLISECONDS)
            }
            else {
                promise.resolve(map)
            }
        }
        tryDeviceInfo()

    }

    @ReactMethod
    fun getUserAgent(promise: Promise) {

        if (!checkReady(promise)) {
            return
        }

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

        if (!checkReady(promise)) {
            return
        }

        var hasPermission = true

        val readPhoneStatePermission: Int = ActivityCompat.checkSelfPermission(reactContext, Manifest.permission.READ_PHONE_STATE)
        if (readPhoneStatePermission != PackageManager.PERMISSION_GRANTED) {
            hasPermission = false
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
    fun exitApp() {
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    @ReactMethod
    fun signIn(name: String, provider: String?) {

        if (!checkReady(null)) {
            return
        }

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
        if (!checkReady(null)) {
            return
        }
        MobclickAgent.onProfileSignOff()
    }

    @ReactMethod
    fun enterPage(pageName: String) {
        if (!checkReady(null)) {
            return
        }
        MobclickAgent.onPageStart(pageName)
    }

    @ReactMethod
    fun leavePage(pageName: String) {
        if (!checkReady(null)) {
            return
        }
        MobclickAgent.onPageEnd(pageName)
    }

    @ReactMethod
    fun sendEvent(eventId: String) {
        if (!checkReady(null)) {
            return
        }
        MobclickAgent.onEvent(reactContext, eventId)
    }

    @ReactMethod
    fun sendEventLabel(eventId: String, label: String) {
        if (!checkReady(null)) {
            return
        }
        MobclickAgent.onEvent(reactContext, eventId, label)
    }

    @ReactMethod
    fun sendEventData(eventId: String, data: ReadableMap) {
        if (!checkReady(null)) {
            return
        }
        val map = data.toHashMap()
        MobclickAgent.onEventObject(reactContext, eventId, map)
    }

    @ReactMethod
    fun sendEventCounter(eventId: String, data: ReadableMap, counter: Int) {
        if (!checkReady(null)) {
            return
        }
        val map = HashMap<String, String>()
        for ((key, value) in data.toHashMap()) {
            map[key] = value as String
        }
        MobclickAgent.onEventValue(reactContext, eventId, map, counter)
    }

    @ReactMethod
    fun sendError(error: String) {
        if (!checkReady(null)) {
            return
        }
        MobclickAgent.reportError(reactContext, error)
    }

    private fun checkReady(promise: Promise?): Boolean {
        if (!isReady) {
            promise?.reject("-1", "umeng sdk is not ready.")
            return false
        }
        return true
    }

    private fun getDeviceInfoMap(): WritableMap {
        // 获取 deviceId 改为三次尝试
        var deviceId = DeviceConfig.getDeviceIdForGeneral(reactContext)
        if (deviceId.isEmpty()) {
            deviceId = Secure.getString(reactContext.contentResolver, Secure.ANDROID_ID)
        }
        if (deviceId.isEmpty()) {
            deviceId = getUUID()
        }
        val deviceType = DeviceConfig.getDeviceType(reactContext)
        val brand = Build.BRAND
        val bundleId = DeviceConfig.getPackageName(reactContext)

        val map = Arguments.createMap()
        map.putString("deviceId", deviceId.lowercase(Locale.ROOT))
        map.putString("deviceType", deviceType.lowercase(Locale.ROOT))
        map.putString("brand", brand.lowercase(Locale.ROOT))
        map.putString("bundleId", bundleId)

        return map
    }

    private fun getUUID(): String {
        val sharedPref = reactContext.currentActivity?.getPreferences(Context.MODE_PRIVATE)
                ?: return ""

        val key = "umeng_analytics_uuid"

        var uuid = sharedPref.getString(key, "")
        if (uuid.isNullOrEmpty()) {
            uuid = UUID.randomUUID().toString()
            sharedPref.edit {
                putString(key, uuid)
            }
        }

        return uuid
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
