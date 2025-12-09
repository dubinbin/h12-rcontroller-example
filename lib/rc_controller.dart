import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:h12controller/global_event.dart';
import 'dart:async';

/// 通过EventBus转发左右摇杆的移动位置，虽然内容简短但不直接将其嵌入在相关页面内主要原因如下：
/// 1. MethodChannel在被多次读取时似乎只会使得第一个接收的读取到，即会拦截掉其他的
/// 2. 将其与页面操纵略微做解耦，便于修改
class RcController {
  static const platform = MethodChannel('com.skydroid2.rcsdk2');

  List<double> location = [];
  String _status = "disconnected";
  Timer? _keyChannelTimer;
  List<int> _joystickValues = [0, 0, 0, 0]; // 存储摇杆值

  // 添加状态获取方法
  String get status => _status;
  List<int> get joystickValues => _joystickValues;

  RcController() {
    setupRc();
    startKeyChannelPolling();
  }

  Future<void> setupRc() async {
    try {
      platform.setMethodCallHandler((call) async {
        if (call.method == 'onRcData') {
          final List<double> values = List<double>.from(call.arguments);
        } else if (call.method == 'onRcStatus') {
          _status = call.arguments as String;
        } else if (call.method == 'onRcKeyChannels') {
          final List<dynamic> rawValues = List<dynamic>.from(call.arguments);
          final List<int> values = rawValues.map((e) => e as int).toList();
          debugPrint("onRcKeyChannels: $values");
          // 处理摇杆值
          if (values.length >= 12) {
            // 将RC Controller的通道值转换为摇杆值
            // 假设前4个通道是摇杆值，需要归一化到[-1, 1]范围
            _joystickValues = [
              values[0], // 右摇杆X ⬅️ ➡️
              values[1], // 左摇杆Y ⬆️ ⬇️
              values[2], // 右摇杆Y ⬆️ ⬇️ (反转)
              values[3], // 左摇杆X ⬅️ ➡️
            ];
            MoveCommand command = MoveCommand(
                _joystickValues[3].toDouble(), // 左X
                _joystickValues[1].toDouble(), // 左Y
                _joystickValues[0].toDouble(), // 右X
                _joystickValues[2].toDouble() // 右Y
                );

            final leftTrigger = values[4];
            final rightTrigger = values[5];
            final scaleTrigger = values[11];
            final audioVolume = values[10];

            myEventBus.fire(command);
            myEventBus.fire(LeftTriggerEvent(leftTrigger));
            myEventBus.fire(RightTriggerEvent(rightTrigger));
            myEventBus.fire(ScaleTriggerEvent(scaleTrigger));
            myEventBus.fire(AudioVolumeEvent(audioVolume));
          }
        } else if (call.method == 'onLeftButtonPressed') {
          myEventBus.fire(LeftButtonPressedEvent());
        } else if (call.method == 'onRightButtonPressed') {
          myEventBus.fire(RightButtonPressedEvent());
        }
      });
      await platform.invokeMethod('setupRc');
    } on PlatformException catch (e) {
      debugPrint("Failed to setup rc: '${e.message}'.");
    }
  }

  /// 获取遥控器按键通道
  Future<List<int>> getRemoteControllerKeyChannel() async {
    try {
      final result =
          await platform.invokeMethod('getRemoteControllerKeyChannel');
      if (result == null) {
        return [];
      }

      // 确保结果是 List 类型
      if (result is List) {
        // 将 List<dynamic> 转换为 List<int>
        return result.map((e) => e is int ? e : 0).toList();
      }

      return [];
    } on PlatformException catch (e) {
      debugPrint("Failed to get RC key channel: '${e.message}'.");
      return [];
    }
  }

  /// 启动定时获取遥控器按键通道
  void startKeyChannelPolling() {
    // 取消可能存在的旧定时器
    _keyChannelTimer?.cancel();

    // 降低轮询频率从300ms到500ms，减少JNI调用频率
    _keyChannelTimer =
        Timer.periodic(const Duration(milliseconds: 300), (timer) {
      try {
        getRemoteControllerKeyChannel();
      } catch (e) {
        debugPrint("RC polling error: $e");
        // 如果连续出错，暂时停止轮询
        if (_consecutiveErrors > 5) {
          debugPrint("Too many RC errors, stopping polling temporarily");
          timer.cancel();
          // 10秒后重试
          Timer(const Duration(seconds: 10), () {
            _consecutiveErrors = 0;
            startKeyChannelPolling();
          });
        } else {
          _consecutiveErrors++;
        }
      }
    });
  }

  // 添加错误计数器
  int _consecutiveErrors = 0;

  /// 停止定时获取遥控器按键通道
  void stopKeyChannelPolling() {
    _keyChannelTimer?.cancel();
    _keyChannelTimer = null;
  }

  /// 释放资源
  void dispose() {
    stopKeyChannelPolling();
  }
}
