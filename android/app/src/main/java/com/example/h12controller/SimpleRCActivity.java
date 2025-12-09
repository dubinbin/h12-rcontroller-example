package com.example.h12controller;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.skydroid.rcsdk.RCSDKManager;
import com.skydroid.rcsdk.common.callback.CompletionCallbackWith;
import com.skydroid.rcsdk.common.callback.SDKManagerCallBack;
import com.skydroid.rcsdk.common.error.SkyException;
import com.skydroid.rcsdk.common.pipeline.Pipeline;
import com.skydroid.rcsdk.common.pipeline.PipelineManager;
import com.skydroid.rcsdk.common.Uart;
import com.skydroid.rcsdk.key.KeyManager;
import com.skydroid.rcsdk.key.RemoteControllerKey;

import java.util.ArrayList;
import java.util.List;

/**
 * 简单的 Android 原生 RC 控制器示例
 * 参考 Flutter 的 main.dart 实现
 * 
 * 功能：
 * 1. 初始化 RC SDK（类似 Flutter 的 setupRc）
 * 2. 定时获取遥控器值（类似 Flutter 的 startKeyChannelPolling，每 300ms）
 * 3. 监听按钮事件
 * 4. 在 UI 上显示所有值（摇杆、拨杆、滚轮、按钮）
 */
public class SimpleRCActivity extends AppCompatActivity {
    private static final String TAG = "SimpleRCActivity";
    
    // ========== UI 组件 ==========
    private TextView tvLeftTrigger;
    private TextView tvRightTrigger;
    private TextView tvScaleTrigger;
    private TextView tvAudioVolume;
    private TextView tvLeftX;
    private TextView tvLeftY;
    private TextView tvRightX;
    private TextView tvRightY;
    private TextView tvLeftButton;
    private TextView tvRightButton;
    
    // ========== RC 控制器相关 ==========
    private Pipeline rcPipeline;
    private Handler handler;
    private Runnable pollingRunnable;
    private static final long POLLING_INTERVAL_MS = 300; // 300ms，与 Flutter 保持一致
    
    // ========== 存储当前值（类似 Flutter 的 State 变量）==========
    private int leftTrigger = 0;
    private int rightTrigger = 0;
    private int scaleTrigger = 0;
    private int audioVolume = 0;
    private double leftX = 0.0;
    private double leftY = 0.0;
    private double rightX = 0.0;
    private double rightY = 0.0;
    private int leftButtonPressed = 0;
    private int rightButtonPressed = 0;
    
    // 用于检测按钮状态变化
    private int lastLeftCButtonValue = 0;
    private int lastRightCButtonValue = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_rc);
        
        // 绑定 UI 组件（类似 Flutter 的 Widget）
        bindViews();
        
        // 初始化 Handler（用于定时轮询）
        handler = new Handler(Looper.getMainLooper());
        
        // 初始化 RC 控制器（类似 Flutter 的 setupRc）
        initRCController();
        
        // 开始定时获取遥控器值（类似 Flutter 的 startKeyChannelPolling）
        startPolling();
    }
    
    /**
     * 绑定 UI 组件
     */
    private void bindViews() {
        tvLeftTrigger = findViewById(R.id.tv_left_trigger);
        tvRightTrigger = findViewById(R.id.tv_right_trigger);
        tvScaleTrigger = findViewById(R.id.tv_scale_trigger);
        tvAudioVolume = findViewById(R.id.tv_audio_volume);
        tvLeftX = findViewById(R.id.tv_left_x);
        tvLeftY = findViewById(R.id.tv_left_y);
        tvRightX = findViewById(R.id.tv_right_x);
        tvRightY = findViewById(R.id.tv_right_y);
        tvLeftButton = findViewById(R.id.tv_left_button);
        tvRightButton = findViewById(R.id.tv_right_button);
    }
    
    /**
     * 初始化 RC 控制器
     * 对应 Flutter 的 setupRc() 方法
     */
    private void initRCController() {
        Log.d(TAG, "初始化 RC 控制器...");
        
        // 初始化 SDK（类似 Flutter 的 platform.invokeMethod('setupRc')）
        RCSDKManager.INSTANCE.initSDK(this, new SDKManagerCallBack() {
            @Override
            public void onRcConnected() {
                Log.d(TAG, "RC 连接成功");
                // 创建并连接 Pipeline
                rcPipeline = PipelineManager.INSTANCE.createPipeline(Uart.UART0);
                PipelineManager.INSTANCE.connectPipeline(rcPipeline);
            }

            @Override
            public void onRcConnectFail(SkyException e) {
                Log.e(TAG, "RC 连接失败: " + (e != null ? e.getMessage() : "unknown error"));
            }

            @Override
            public void onRcDisconnect() {
                Log.d(TAG, "RC 断开连接");
                if (rcPipeline != null) {
                    PipelineManager.INSTANCE.disconnectPipeline(rcPipeline);
                    rcPipeline = null;
                }
            }
        });

        RCSDKManager.INSTANCE.setMainThreadCallBack(true);
        RCSDKManager.INSTANCE.connectToRC();
    }
    
    /**
     * 开始定时轮询获取遥控器值
     * 对应 Flutter 的 startKeyChannelPolling() 方法
     * Flutter 使用 Timer.periodic(Duration(milliseconds: 300))
     * Android 使用 Handler.postDelayed()
     */
    private void startPolling() {
        if (pollingRunnable != null) {
            handler.removeCallbacks(pollingRunnable);
        }
        
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                // 获取遥控器按键通道值（类似 Flutter 的 getRemoteControllerKeyChannel()）
                getRemoteControllerKeyChannel();
                // 继续下一次轮询
                handler.postDelayed(this, POLLING_INTERVAL_MS);
            }
        };
        
        handler.post(pollingRunnable);
    }
    
    /**
     * 停止轮询
     * 对应 Flutter 的 stopKeyChannelPolling()
     */
    private void stopPolling() {
        if (pollingRunnable != null) {
            handler.removeCallbacks(pollingRunnable);
            pollingRunnable = null;
        }
    }
    
    /**
     * 获取遥控器按键通道值
     * 对应 Flutter 的 getRemoteControllerKeyChannel() 方法
     */
    private void getRemoteControllerKeyChannel() {
        try {
            KeyManager.INSTANCE.get(RemoteControllerKey.INSTANCE.getKeyChannels(), 
                new CompletionCallbackWith<int[]>() {
                    @Override
                    public void onSuccess(int[] values) {
                        // 在主线程更新 UI（类似 Flutter 的 setState）
                        runOnUiThread(() -> {
                            handleRCKeyChannels(values);
                        });
                    }

                    @Override
                    public void onFailure(SkyException e) {
                        Log.e(TAG, "获取遥控器按键通道失败: " + e.getMessage(), e);
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "获取遥控器按键通道异常: " + e.getMessage(), e);
        }
    }
    
    /**
     * 处理遥控器按键通道数据
     * 对应 Flutter 的 onRcKeyChannels 回调处理
     * 类似 Flutter 中 platform.setMethodCallHandler 的 'onRcKeyChannels' 分支
     */
    private void handleRCKeyChannels(int[] values) {
        if (values == null || values.length < 12) {
            return;
        }
        
        // 解析摇杆值（与 Flutter 端保持一致）
        // values[0]: 右摇杆X ⬅️ ➡️
        // values[1]: 左摇杆Y ⬆️ ⬇️
        // values[2]: 右摇杆Y ⬆️ ⬇️
        // values[3]: 左摇杆X ⬅️ ➡️
        rightX = values[0];
        leftY = values[1];
        rightY = values[2];
        leftX = values[3];
        
        // 解析拨杆和滚轮值
        leftTrigger = values[4];   // 左拨杆
        rightTrigger = values[5];  // 右拨杆
        audioVolume = values[10];   // 左滚轮（音频音量）
        scaleTrigger = values[11];  // 右滚轮（缩放）
        
        // 检测按钮按下（类似 Flutter 的 onLeftButtonPressed / onRightButtonPressed）
        int leftCButton = values[8];
        int rightCButton = values[9];
        
        // 左按钮按下检测（类似 Flutter 的 LeftButtonPressedEvent）
        if (leftCButton != lastLeftCButtonValue) {
            if (leftCButton == 1950 || leftCButton == 1050) {
                leftButtonPressed++;
                Log.d(TAG, "左按钮按下，总计: " + leftButtonPressed);
            }
            lastLeftCButtonValue = leftCButton;
        }
        
        // 右按钮按下检测（类似 Flutter 的 RightButtonPressedEvent）
        if (rightCButton != lastRightCButtonValue) {
            if (rightCButton == 1950 || rightCButton == 1050) {
                rightButtonPressed++;
                Log.d(TAG, "右按钮按下，总计: " + rightButtonPressed);
            }
            lastRightCButtonValue = rightCButton;
        }
        
        // 更新 UI（类似 Flutter 的 setState）
        updateUI();
    }
    
    /**
     * 更新 UI 显示
     * 对应 Flutter 的 setState() 方法
     */
    private void updateUI() {
        // 更新摇杆值（类似 Flutter 的 myEventBus.on<MoveCommand>()）
        tvLeftX.setText("左摇杆X: " + leftX);
        tvLeftY.setText("左摇杆Y: " + leftY);
        tvRightX.setText("右摇杆X: " + rightX);
        tvRightY.setText("右摇杆Y: " + rightY);
        
        // 更新拨杆值（类似 Flutter 的 myEventBus.on<LeftTriggerEvent>() 等）
        tvLeftTrigger.setText("左拨杆值: " + leftTrigger);
        tvRightTrigger.setText("右拨杆值: " + rightTrigger);
        
        // 更新滚轮值（类似 Flutter 的 myEventBus.on<ScaleTriggerEvent>() 等）
        tvScaleTrigger.setText("右滚轮值: " + scaleTrigger);
        tvAudioVolume.setText("左滚轮值: " + audioVolume);
        
        // 更新按钮计数（类似 Flutter 的 myEventBus.on<LeftButtonPressedEvent>() 等）
        tvLeftButton.setText("左按键: " + leftButtonPressed + " 次");
        tvRightButton.setText("右按键: " + rightButtonPressed + " 次");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 停止轮询（类似 Flutter 的 dispose()）
        stopPolling();
        
        // 断开 RC 连接
        RCSDKManager.INSTANCE.disconnectRC();
        if (rcPipeline != null) {
            PipelineManager.INSTANCE.disconnectPipeline(rcPipeline);
            rcPipeline = null;
        }
    }
}

