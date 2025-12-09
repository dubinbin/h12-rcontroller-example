package com.example.h12controller;

import android.os.Bundle;
import android.view.KeyEvent;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

import java.util.List;

public class MainActivity extends FlutterActivity {

    private static final String RC_CHANNEL = "com.skydroid2.rcsdk2";

    private MethodChannel rcChannel;
    private RCControllerManager rcControllerManager;

    @Override
    public void configureFlutterEngine(FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        
        // ⚠️ 重要：注册所有 Flutter 插件
        io.flutter.plugins.GeneratedPluginRegistrant.registerWith(flutterEngine);
        
        rcChannel = new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), RC_CHANNEL);
        
        rcControllerManager = new RCControllerManager(this);

        rcControllerManager.setButtonEventListener(new ButtonEventListener() {
            @Override
            public void onLeftButtonPressed() {

                rcChannel.invokeMethod("onLeftButtonPressed", null);
            }
            
            @Override
            public void onRightButtonPressed() {
                rcChannel.invokeMethod("onRightButtonPressed", null);
            }
        });

        rcChannel.setMethodCallHandler(
            (call, result) -> {
                if ("setupRc".equals(call.method)) {
                    rcControllerManager.initRCSdk();
                    result.success(null);
                } else if ("getRemoteControllerKeyChannel".equals(call.method)) {
                    final List<Integer> values = rcControllerManager.getRemoteControllerKeyChannel();
                    result.success(values);
                } else {
                    result.notImplemented();
                }
            });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        rcControllerManager.disconnect();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return super.dispatchKeyEvent(event);
    }

    // Getters for RCControllerManager
    public MethodChannel getRcChannel() {
        return rcChannel;
    }

}