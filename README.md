# @react-native-hero/umeng-analytics

## Getting started

Install the library using either Yarn:

```
yarn add @react-native-hero/umeng-analytics
```

or npm:

```
npm install --save @react-native-hero/umeng-analytics
```

## Link

- React Native v0.60+

For iOS, use `cocoapods` to link the package.

run the following command:

```
$ cd ios && pod install
```

For android, the package will be linked automatically on build.

- React Native <= 0.59

run the following command to link the package:

```
$ react-native link @react-native-hero/umeng-analytics
```

## Setup

![image](https://user-images.githubusercontent.com/2732303/77606227-ded8b680-6f51-11ea-9aa4-0378e79deaa7.png)

打开应用信息页面，安卓推送有 `Appkey` 和 `Umeng Message Secret` 两个字段，iOS 只有 `Appkey` 字段，后面将用这些字段初始化友盟。

### iOS

修改 `AppDelegate.m`，如下

```oc
#import <RNTUmengAnalytics.h>

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
  ...
  // 初始化友盟基础库
  // channel 一般填 App Store，如果有测试环境，可按需填写
  // debug 表示是否打印调试信息
  [RNTUmengAnalytics init:@"appKey" channel:@"App Store" debug:false];
  return YES;
}
```

### Android

修改 `android/build.gradle`，如下：

```
allprojects {
    repositories {
        // 确保添加了友盟仓库
        maven { url 'https://repo1.maven.org/maven2/' }
    }
}
```

`android/app/build.gradle` 根据不同的包填写不同的配置，如下：

```
buildTypes {
    debug {
        manifestPlaceholders = [
            UMENG_APP_KEY: '',
            UMENG_PUSH_SECRET: '',
            UMENG_CHANNEL: '',
        ]
    }
    release {
        manifestPlaceholders = [
            UMENG_APP_KEY: '',
            UMENG_PUSH_SECRET: '',
            UMENG_CHANNEL: '',
        ]
    }
}
```

在 `MainApplication` 的 `onCreate` 方法进行初始化，如下：

Kotlin 版本

```kotlin
override fun onCreate() {
    val metaData = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).metaData

    // 初始化友盟基础库
    // 第三个参数表示是否显示调试信息
    RNTUmengAnalyticsModule.init(this, metaData, false)
}
```

Java 版本

```java
// onCreate 方法体换成下面这段
Bundle metaData  = this.getPackageManager().getApplicationInfo(
  this.getPackageName(), PackageManager.GET_META_DATA
).metaData;
RNTUmengAnalyticsModule.init(this, metaData, false);  
```

### 配置混淆规则

在 `android/app/proguard-rules.pro` 添加以下混淆规则，注意替换自己的包名，并且删掉 `[` 和 `]`。

```
-keep public class [您的应用包名].R$*{
public static final int *;
}
```

## Usage

```js
import {
  // 初始化友盟时传入的 channel 参数
  CHANNEL,

  init,
  getDeviceInfo,
  signIn,
  signOut,
  exitApp,
  enterPage,
  leavePage,
  sendEvent,
  sendEventLabel,
  sendEventData,
  sendEventCounter,
} from '@react-native-hero/umeng-analytics'

// 对于安卓来说，需要等用户同意隐私政策后，再调用 init，js 的 init 才是真正的初始化
// https://developer.umeng.com/docs/119267/detail/182050
// 对于 ios 来说，不调用 init 也没问题
init()

// 提供一个退出 app 的方法
// 好像 RN 官方也没提供此方法，单个方法不好写一个库，就放在这个库了
exitApp()

// 集成测试，获取设备信息
getDeviceInfo().then(data => {
  data.deviceId
  // mac is android only
  data.mac
})

// 帐号统计
signIn('userId')
// provider 不能以下划线开头，使用大写字母和数字标识
// 如果是上市公司，建议使用股票代码，比如 WB
signIn('userId', 'provider')
signOut()

// 页面统计，注意要配对调用
// 不能连续调用 enterPage，也不能连续调用 leavePage
enterPage('pageName')
leavePage('pageName')

// 自定义事件，eventId 需先在友盟后台注册之后才可以统计
sendEvent('eventId')
sendEventLabel('eventId', 'label')
sendEventData('eventId', { key1: 'value1', key2: 'value2' })
sendEventCounter('eventId', { key1: 'value1', key2: 'value2' }, 1)
```
