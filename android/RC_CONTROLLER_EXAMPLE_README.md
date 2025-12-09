# RC 遥控器 Java 示例使用说明

这个示例展示了如何在纯 Java Android 项目中初始化和获取 RC 遥控器的值，类似于 Flutter 的 `main.dart` 功能。

## 文件说明

1. **RCControllerExampleActivity.java** - 主 Activity（纯 Java 版本），展示如何：
   - 初始化 RC SDK（不依赖 Flutter）
   - 定时获取遥控器值（每 300ms）
   - 在 UI 上显示摇杆、拨杆、滚轮的值

2. **activity_rc_example.xml** - 对应的布局文件，显示所有遥控器数据

3. **RCControllerManager.java** - RC 控制器管理器（Flutter 版本，仅供参考）

**注意**：这个示例是纯 Java 实现，不依赖 Flutter 或 RCControllerManager。它直接使用 RCSDK 的 API。

## 使用方法

### 1. 在 AndroidManifest.xml 中注册 Activity

```xml
<activity
    android:name=".RCControllerExampleActivity"
    android:label="RC Controller Example"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

### 2. 核心代码流程

#### 初始化（在 onCreate 中）

```java
// 1. 初始化 RC SDK（纯 Java 版本，直接使用 RCSDK API）
RCSDKManager.INSTANCE.initSDK(this, new SDKManagerCallBack() {
    @Override
    public void onRcConnected() {
        // 连接成功，创建 Pipeline
        rcPipeline = PipelineManager.INSTANCE.createPipeline(Uart.UART0);
        PipelineManager.INSTANCE.connectPipeline(rcPipeline);
    }
    // ... 其他回调
});

RCSDKManager.INSTANCE.setMainThreadCallBack(true);
RCSDKManager.INSTANCE.connectToRC();

// 2. 开始定时获取遥控器值（每 300ms）
startPolling();
```

#### 获取遥控器值

```java
private void getRemoteControllerKeyChannel() {
    KeyManager.INSTANCE.get(RemoteControllerKey.INSTANCE.getKeyChannels(), 
        new CompletionCallbackWith<int[]>() {
            @Override
            public void onSuccess(int[] values) {
                // 处理遥控器数据
                handleRCKeyChannels(values);
            }

            @Override
            public void onFailure(SkyException e) {
                // 处理错误
            }
        });
}
```

#### 数据解析

遥控器数据数组 `values` 包含至少 12 个元素：

- `values[0]` - 右摇杆 X（左右）
- `values[1]` - 左摇杆 Y（上下）
- `values[2]` - 右摇杆 Y（上下）
- `values[3]` - 左摇杆 X（左右）
- `values[4]` - 左拨杆
- `values[5]` - 右拨杆
- `values[10]` - 左滚轮（音频音量）
- `values[11]` - 右滚轮（缩放）

## 与 Flutter 版本的对比

| Flutter (main.dart) | Java (RCControllerExampleActivity) |
|---------------------|-------------------------------------|
| `RcController().setupRc()` | `rcControllerManager.initRCSdk()` |
| `getRemoteControllerKeyChannel()` | `getRemoteControllerKeyChannel()` |
| `Timer.periodic(300ms)` | `Handler.postDelayed(300ms)` |
| `myEventBus.fire()` | 直接更新 UI |
| `setState()` | `updateUI()` |

## 注意事项

1. **轮询频率**：当前设置为 300ms，与 Flutter 版本保持一致。可以根据需要调整 `POLLING_INTERVAL_MS`。

2. **线程安全**：`onSuccess` 回调可能不在主线程，需要使用 `runOnUiThread()` 更新 UI。

3. **资源释放**：在 `onDestroy()` 中记得：
   - 停止轮询
   - 断开 RC 连接

4. **连接状态**：当前示例通过日志观察连接状态。如果需要 UI 显示，可以：
   - 在 `RCControllerManager` 中添加连接状态回调接口
   - 或者在 Activity 中监听日志

## 扩展功能

如果需要监听连接状态变化，可以修改 `RCControllerManager`：

```java
public interface RCConnectionListener {
    void onConnected();
    void onDisconnected();
    void onConnectionFailed(String error);
}
```

然后在 `initRCSdk()` 的回调中调用这些方法。

