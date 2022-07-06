
import { NativeModules } from 'react-native'

const { RNTUmengAnalytics } = NativeModules

// 初始化时配置的渠道
export const CHANNEL = RNTUmengAnalytics.CHANNEL

export function init() {
  return RNTUmengAnalytics.init()
}

export function getDeviceInfo() {
  return RNTUmengAnalytics.getDeviceInfo()
}

export function getUserAgent() {
  return RNTUmengAnalytics.getUserAgent()
}

export function getPhoneNumber() {
  return RNTUmengAnalytics.getPhoneNumber()
}

export function signIn(userId, provider) {
  RNTUmengAnalytics.signIn(userId, provider)
}

export function signOut() {
  RNTUmengAnalytics.signOut()
}

export function exitApp() {
  RNTUmengAnalytics.exitApp()
}

// enterPage 和 leavePage 必须对称调用
// 在这里做一层保证
let currentPage

export function enterPage(pageName) {
  if (!currentPage) {
    RNTUmengAnalytics.enterPage(pageName)
    currentPage = pageName
  }
}

export function leavePage(pageName) {
  if (currentPage === pageName) {
    RNTUmengAnalytics.leavePage(pageName)
    currentPage = undefined
  }
}

// 友盟文档规定：id，ts，du 是保留字段，不能作为 event id 及 key 的名称。
const bannedKeys = {
  id: true,
  ts: true,
  du: true,
}

function checkEventId(eventId) {
  if (bannedKeys[eventId]) {
    throw new Error(`[${eventId}] 是保留字段，不能作为 event id.`)
  }
}
function checkEventDataKey(data) {
  for (let key in data) {
    if (bannedKeys[key]) {
      throw new Error(`${key} 是保留字段，不能作为 event data 的 key.`)
    }
  }
}

export function sendEvent(eventId) {
  checkEventId(eventId)
  RNTUmengAnalytics.sendEvent(eventId)
}

export function sendEventLabel(eventId, label) {
  checkEventId(eventId)
  RNTUmengAnalytics.sendEventLabel(eventId, label)
}

export function sendEventData(eventId, data) {
  checkEventId(eventId)
  checkEventDataKey(data)
  RNTUmengAnalytics.sendEventData(eventId, data)
}

export function sendEventCounter(eventId, data, counter) {
  checkEventId(eventId)
  checkEventDataKey(data)
  RNTUmengAnalytics.sendEventCounter(eventId, data, counter)
}

export function sendError(error) {
  // 安卓才有这个接口
  if (RNTUmengAnalytics.sendError) {
    RNTUmengAnalytics.sendError(error)
  }
}
