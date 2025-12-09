package com.example.h12controller;

import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;

import com.skydroid.rcsdk.*;
import com.skydroid.rcsdk.common.callback.*;
import com.skydroid.rcsdk.common.error.SkyException;
import com.skydroid.rcsdk.common.pipeline.Pipeline;
import com.skydroid.rcsdk.key.RemoteControllerKey;
import com.skydroid.rcsdk.common.Uart;
import com.skydroid.rcsdk.comm.CommListener;

import java.util.ArrayList;
import java.util.List;

public class RCControllerManager {
    private static final String TAG = "RCControllerManager";
    public interface ButtonEventListener {
        void onLeftButtonPressed();
        void onRightButtonPressed();
    }

    private final MainActivity activity;
    private Pipeline rcPipeline;
    private int lastLeftCButtonValue = 0;  // 记录上一次的按钮值
    private int lastRightCButtonValue = 0;
    private ButtonEventListener buttonEventListener;  // 按钮事件监听器

    public RCControllerManager(MainActivity activity) {
        this.activity = activity;
    }
    
    /**
     * 设置按钮事件监听器
     * @param listener 按钮事件监听器
     */
    public void setButtonEventListener(ButtonEventListener listener) {
        this.buttonEventListener = listener;
    }

    public void initRCSdk() {
        RCSDKManager.INSTANCE.initSDK(activity, new SDKManagerCallBack() {
            @Override
            public void onRcConnected() {
                Log.d(TAG, "onRcConnected");
                rcPipeline = PipelineManager.INSTANCE.createPipeline(Uart.UART0);
                rcPipeline.setOnCommListener(getRCCommListener(0, "数传管道"));
                PipelineManager.INSTANCE.connectPipeline(rcPipeline);
            }

            @Override
            public void onRcConnectFail(SkyException e) {
                Log.e(TAG, "RC connection failed: " + (e != null ? e.getMessage() : "unknown error"));
            }

            @Override
            public void onRcDisconnect() {
                Log.d(TAG, "RC disconnected");
                if (rcPipeline != null) {
                    PipelineManager.INSTANCE.disconnectPipeline(rcPipeline);
                    rcPipeline = null;
                }
            }
        });

        RCSDKManager.INSTANCE.setMainThreadCallBack(true);
        RCSDKManager.INSTANCE.connectToRC();
    }

    public List<Integer> getRemoteControllerKeyChannel() {
        try {
            KeyManager.INSTANCE.get(RemoteControllerKey.INSTANCE.getKeyChannels(), new CompletionCallbackWith<int[]>() {
                @Override
                public void onSuccess(int[] value) {
                    try {
                        List<Integer> result = new ArrayList<>();
                        for (int i : value) {
                            result.add(i);
                        }
                        
                        if (value.length >= 12) {
                            handleRCInput(value);
                        }
                        
                        activity.getRcChannel().invokeMethod("onRcKeyChannels", result);
                    } catch (Exception e) {
                        Log.e(TAG, "Error in onSuccess callback: " + e.getMessage(), e);
                        activity.getRcChannel().invokeMethod("onRcKeyChannels", new ArrayList<Integer>());
                    }
                }

                @Override
                public void onFailure(SkyException e) {
                    // 如果获取遥控器按键通道失败，不太影响实际获取。
                    Log.e(TAG, "RC Key channels failure: " + e.getMessage(), e);
                    activity.getRcChannel().invokeMethod("onRcKeyChannels", new ArrayList<Integer>());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in getRemoteControllerKeyChannel: " + e.getMessage(), e);
            return new ArrayList<Integer>();
        }
        return new ArrayList<Integer>();
    }

    private void handleRCInput(int[] value) {
        try {
            int leftX = value[3];
            int leftY = value[1];
            int rightX = value[0];
            int rightY = value[2];

            int LeftCButton = value[8];
            int RightCButton = value[9];

            long eventTime = SystemClock.uptimeMillis();
            int keyCode = determineKeyCode(leftX, leftY, false);  // 左摇杆
            int rightKeyCode = determineKeyCode(rightX, rightY, true);  // 右摇杆
        
            // 只有当按钮值发生变化时才处理
            if (LeftCButton != lastLeftCButtonValue) {
                if (LeftCButton == 1950 || LeftCButton == 1050) {

                    if (buttonEventListener != null) {
                        buttonEventListener.onLeftButtonPressed();
                    }
                    keyCode = KeyEvent.KEYCODE_BUTTON_L2;
                    Log.d(TAG, "click event keycode: " + keyCode);
                }
                lastLeftCButtonValue = LeftCButton;
            }

            if (RightCButton != lastRightCButtonValue) {
                if (RightCButton == 1950 || RightCButton == 1050) {
                    if (buttonEventListener != null) {
                        buttonEventListener.onRightButtonPressed();
                    }
                    keyCode = KeyEvent.KEYCODE_BUTTON_R2;
                    Log.d(TAG, "click event keycode: " + keyCode);
                }
                lastRightCButtonValue = RightCButton;
            }

            if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                sendKeyEvent(eventTime, keyCode);
            }
            if (rightKeyCode != KeyEvent.KEYCODE_UNKNOWN) {
                sendKeyEvent(eventTime, rightKeyCode);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in handleRCInput: " + e.getMessage(), e);
        }
    }

    private int determineKeyCode(int x, int y, boolean isRightJoystick) {
        if (isRightJoystick) {
            // 左
            if (x < 1500 && x > 1350) {
                // 1档位
                return KeyEvent.KEYCODE_NUMPAD_7;
            }
            if (x < 1350 && x > 1200) {
                 // 2档位
                return KeyEvent.KEYCODE_NUMPAD_8;
            }
            if (x < 1200 && x >= 1050) {
                // 3档位
                return KeyEvent.KEYCODE_NUMPAD_9;
            }

            // 右
            if (x > 1500 && x < 1650) {
                // 1档位
                return KeyEvent.KEYCODE_NUMPAD_DIVIDE;
            }
            if (x > 1650 && x < 1800) {
                // 2档位
                return KeyEvent.KEYCODE_NUMPAD_MULTIPLY;
            }
            if (x > 1800 && x <= 1950) {
                // 3档位
                return KeyEvent.KEYCODE_NUMPAD_SUBTRACT;
            }

            // 下摇杆
            if (y < 1500 && y > 1350) {
                // 1档位
                return KeyEvent.KEYCODE_NUMPAD_4;
            }
            if (y > 1350 && y < 1200) {
                // 2档位
                return KeyEvent.KEYCODE_NUMPAD_5;
            }
            if (y < 1200 && y >= 1050) {
                // 3档位
                return KeyEvent.KEYCODE_NUMPAD_6;
            }

            // 上摇杆
            if (y > 1500 && y < 1650) {
                // 1档位
                return KeyEvent.KEYCODE_NUMPAD_1;
            }
            if (y > 1650 && y < 1800) {
                // 2档位
                return KeyEvent.KEYCODE_NUMPAD_2;
            }
            if (y > 1800 && y <= 1950) {
                // 3档位
                return KeyEvent.KEYCODE_NUMPAD_3;
            }

        } else {
            // 左摇杆保持原样
            if (x < 1400) return KeyEvent.KEYCODE_DPAD_LEFT;
            if (x > 1600) return KeyEvent.KEYCODE_DPAD_RIGHT;
            if (y < 1400) return KeyEvent.KEYCODE_DPAD_UP;
            if (y > 1600) return KeyEvent.KEYCODE_DPAD_DOWN;
        }
        return KeyEvent.KEYCODE_UNKNOWN;
    }

    private void sendKeyEvent(long eventTime, int keyCode) {
        KeyEvent downEvent = new KeyEvent(eventTime, eventTime,
            KeyEvent.ACTION_DOWN, keyCode, 0,12,0,0,0,-1);
        boolean downResult = activity.dispatchKeyEvent(downEvent);
        Log.d(TAG, "Key down event dispatched: " + downResult + " for key: " + keyCode);
        
        KeyEvent upEvent = new KeyEvent(eventTime, eventTime,
            KeyEvent.ACTION_UP, keyCode, 0,12,0,0,0,-1);
        boolean upResult = activity.dispatchKeyEvent(upEvent);
        Log.d(TAG, "Key up event dispatched: " + upResult + " for key: " + keyCode);
    }

    public void disconnect() {
        RCSDKManager.INSTANCE.disconnectRC();
        if (rcPipeline != null) {
            PipelineManager.INSTANCE.disconnectPipeline(rcPipeline);
        }
    }

    private CommListener getRCCommListener(int type, String tag) {
        return new CommListener() {
            @Override
            public void onConnectSuccess() {
                Log.d(TAG, tag + " 连接成功");
            }

            @Override
            public void onConnectFail(SkyException e) {
                Log.d(TAG, tag + " 连接失败" + e);
            }

            @Override
            public void onDisconnect() {
                Log.d(TAG, tag + " 断开连接");
            }

            @Override
            public void onReadData(byte[] bytes) {
                if (type == 0) {
                    return;
                }
            }
        };
    }
} 