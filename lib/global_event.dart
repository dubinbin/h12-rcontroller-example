import 'package:event_bus/event_bus.dart';

EventBus myEventBus = EventBus();

class LeftTriggerEvent {
  final int leftTrigger;
  LeftTriggerEvent(this.leftTrigger);
}

class LeftButtonPressedEvent {
  LeftButtonPressedEvent();
}

class RightButtonPressedEvent {
  RightButtonPressedEvent();
}

class ScaleTriggerEvent {
  final int scaleTrigger;
  ScaleTriggerEvent(this.scaleTrigger);
}

class AudioVolumeEvent {
  final int audioVolume;
  AudioVolumeEvent(this.audioVolume);
}

class RightTriggerEvent {
  final int rightTrigger;
  RightTriggerEvent(this.rightTrigger);
}

// 定义移动指令类
class MoveCommand {
  final double leftX;
  final double leftY;
  final double rightX;
  final double rightY;

  MoveCommand(this.leftX, this.leftY, this.rightX, this.rightY);
}
