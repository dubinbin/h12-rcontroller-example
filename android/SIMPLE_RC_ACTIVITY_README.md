# 简单的 Android 原生 RC 控制器示例

这是一个简单的 Android 原生示例，参考 Flutter 的 `main.dart` 实现，供 Android 开发同事参考。

## 文件说明

1. **SimpleRCActivity.java** - 主 Activity，展示如何初始化和获取 RC 遥控器的值
2. **activity_simple_rc.xml** - 对应的布局文件

## 与 Flutter 版本的对应关系

| Flutter (main.dart / rc_controller.dart) | Android (SimpleRCActivity.java) |
|------------------------------------------|----------------------------------|
| `RcController().setupRc()` | `initRCController()` |
| `Timer.periodic(300ms)` | `Handler.postDelayed(300ms)` |
| `getRemoteControllerKeyChannel()` | `getRemoteControllerKeyChannel()` |
| `myEventBus.on<MoveCommand>()` | `handleRCKeyChannels()` 中更新摇杆值 |
| `myEventBus.on<LeftTriggerEvent>()` | `handleRCKeyChannels()` 中更新 `leftTrigger` |
| `myEventBus.on<RightTriggerEvent>()` | `handleRCKeyChannels()` 中更新 `rightTrigger` |
| `myEventBus.on<ScaleTriggerEvent>()` | `handleRCKeyChannels()` 中更新 `scaleTrigger` |
| `myEventBus.on<AudioVolumeEvent>()` | `handleRCKeyChannels()` 中更新 `audioVolume` |
| `myEventBus.on<LeftButtonPressedEvent>()` | `handleRCKeyChannels()` 中检测并更新 `leftButtonPressed` |
| `myEventBus.on<RightButtonPressedEvent>()` | `handleRCKeyChannels()` 中检测并更新 `rightButtonPressed` |
| `setState()` | `updateUI()` |

## 核心流程

### 1. 初始化（onCreate）

```java
// 1. 绑定 UI 组件
bindViews();

// 2. 初始化 Handler（用于定时轮询）
handler = new Handler(Looper.getMainLooper());

// 3. 初始化 RC SDK
initRCController();

// 4. 开始定时获取遥控器值（每 300ms）
startPolling();
```

### 2. 初始化 RC SDK

```java
private void initRCController() {
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
}
```

### 3. 定时轮询获取值

```java
private void startPolling() {
    pollingRunnable = new Runnable() {
        @Override
        public void run() {
            getRemoteControllerKeyChannel();  // 获取遥控器值
            handler.postDelayed(this, 300);   // 300ms 后再次执行
        }
    };
    handler.post(pollingRunnable);
}
```

### 4. 获取遥控器值

```java
private void getRemoteControllerKeyChannel() {
    KeyManager.INSTANCE.get(RemoteControllerKey.INSTANCE.getKeyChannels(), 
        new CompletionCallbackWith<int[]>() {
            @Override
            public void onSuccess(int[] values) {
                runOnUiThread(() -> {
                    handleRCKeyChannels(values);  // 处理数据并更新 UI
                });
            }
            // ...
        });
}
```

### 5. 处理数据并更新 UI

```java
private void handleRCKeyChannels(int[] values) {
    // 解析数据
    rightX = values[0];      // 右摇杆X
    leftY = values[1];        // 左摇杆Y
    rightY = values[2];       // 右摇杆Y
    leftX = values[3];        // 左摇杆X
    leftTrigger = values[4];  // 左拨杆
    rightTrigger = values[5]; // 右拨杆
    audioVolume = values[10]; // 左滚轮
    scaleTrigger = values[11]; // 右滚轮
    
    // 检测按钮按下
    int leftCButton = values[8];
    int rightCButton = values[9];
    // ... 按钮检测逻辑
    
    // 更新 UI
    updateUI();
}
```

## 数据映射

遥控器数据数组 `values` 包含至少 12 个元素：

- `values[0]` - 右摇杆 X（左右）
- `values[1]` - 左摇杆 Y（上下）
- `values[2]` - 右摇杆 Y（上下）
- `values[3]` - 左摇杆 X（左右）
- `values[4]` - 左拨杆
- `values[5]` - 右拨杆
- `values[8]` - 左按钮（C 按钮）
- `values[9]` - 右按钮（C 按钮）
- `values[10]` - 左滚轮（音频音量）
- `values[11]` - 右滚轮（缩放）

## 使用方法

1. 在 `AndroidManifest.xml` 中注册 Activity：

```xml
<activity
    android:name=".SimpleRCActivity"
    android:label="Simple RC Example"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

2. 运行应用，Activity 会自动：
   - 初始化 RC SDK
   - 每 300ms 获取一次遥控器值
   - 实时更新 UI 显示

## 注意事项

1. **轮询频率**：当前设置为 300ms，与 Flutter 版本保持一致
2. **线程安全**：`onSuccess` 回调可能不在主线程，需要使用 `runOnUiThread()` 更新 UI
3. **资源释放**：在 `onDestroy()` 中记得停止轮询和断开连接
4. **按钮检测**：通过比较当前值和上一次值来检测按钮按下事件

## 与 Flutter 版本的主要区别

1. **事件总线**：Flutter 使用 `EventBus`，Android 原生直接在主线程更新 UI
2. **定时器**：Flutter 使用 `Timer.periodic`，Android 使用 `Handler.postDelayed`
3. **状态管理**：Flutter 使用 `setState()`，Android 直接调用 `updateUI()` 方法

