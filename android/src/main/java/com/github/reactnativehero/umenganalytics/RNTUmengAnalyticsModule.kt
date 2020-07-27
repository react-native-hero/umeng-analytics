package com.github.reactnativehero.umenganalytics

import android.app.Application
import android.os.Bundle
import com.facebook.react.bridge.*
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure
import com.umeng.commonsdk.statistics.common.DeviceConfig
import java.lang.Exception

class RNTUmengAnalyticsModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), LifecycleEventListener {

    companion object {

        private var channel = ""

        // 初始化友盟基础库
        @JvmStatic fun init(app: Application, metaData: Bundle, debug: Boolean) {

            val appKey = metaData.getString("UMENG_APP_KEY", "").trim()
            val pushSecret = metaData.getString("UMENG_PUSH_SECRET", "").trim()
            
            channel = metaData.getString("UMENG_CHANNEL", "").trim()

            UMConfigure.setLogEnabled(debug)
            UMConfigure.init(app, appKey, channel, UMConfigure.DEVICE_TYPE_PHONE, pushSecret)

        }

        // 初始化友盟统计
        @JvmStatic fun analytics(app: Application) {
            // 手动采集
            MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.MANUAL)
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
    fun getDeviceInfo(promise: Promise) {

        val deviceId = DeviceConfig.getDeviceIdForGeneral(reactContext)
        val mac = DeviceConfig.getMac(reactContext)

        val map = Arguments.createMap()
        map.putString("deviceId", deviceId)
        map.putString("mac", mac)

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