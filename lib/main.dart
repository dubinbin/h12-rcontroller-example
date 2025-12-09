import 'package:flutter/material.dart';
import 'package:h12controller/global_event.dart';
import 'package:h12controller/rc_controller.dart';

// 全局RC控制器实例
RcController globalRcController = RcController();

void main() {
  runApp(const MyApp());

  // 初始化RC控制器 - 使用非阻塞方式
  globalRcController.setupRc().catchError((error) {
    debugPrint('RC控制器初始化失败: $error');
  });
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});
  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  int _leftTrigger = 0;
  int _rightTrigger = 0;
  double _scaleTrigger = 0.0;
  double _audioVolume = 0.0;

  double _leftX = 0.0;
  double _leftY = 0.0;
  double _rightX = 0.0;
  double _rightY = 0.0;

  int _leftButtonPressed = 0;
  int _rightButtonPressed = 0;

  @override
  void dispose() {
    globalRcController.dispose();
    super.dispose();
  }

  @override
  void initState() {
    super.initState();
    myEventBus.on<LeftTriggerEvent>().listen((event) {
      setState(() {
        _leftTrigger = event.leftTrigger;
      });
    });
    myEventBus.on<RightTriggerEvent>().listen((event) {
      setState(() {
        _rightTrigger = event.rightTrigger;
      });
    });
    myEventBus.on<ScaleTriggerEvent>().listen((event) {
      setState(() {
        _scaleTrigger = event.scaleTrigger.toDouble();
      });
    });
    myEventBus.on<AudioVolumeEvent>().listen((event) {
      setState(() {
        _audioVolume = event.audioVolume.toDouble();
      });
    });
    myEventBus.on<LeftButtonPressedEvent>().listen((event) {
      setState(() {
        _leftButtonPressed = _leftButtonPressed + 1;
      });
    });
    myEventBus.on<RightButtonPressedEvent>().listen((event) {
      setState(() {
        _rightButtonPressed = _rightButtonPressed + 1;
      });
    });

    myEventBus.on<MoveCommand>().listen((event) {
      debugPrint(
          'MoveCommand: ${event.leftX}, ${event.leftY}, ${event.rightX}, ${event.rightY}');
      setState(() {
        _leftX = event.leftX;
        _leftY = event.leftY;
        _rightX = event.rightX;
        _rightY = event.rightY;
      });
    });
  }

  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Text(widget.title),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Text(
              '左拨杆值: $_leftTrigger',
            ),
            Text(
              '右拨杆值: $_rightTrigger',
            ),
            Text(
              '右滚轮值: $_scaleTrigger',
            ),
            Text(
              '左滚轮值: $_audioVolume',
            ),
            Text(
              '左摇杆X: $_leftX',
            ),
            Text(
              '左摇杆Y: $_leftY',
            ),
            Text(
              '右摇杆X: $_rightX',
            ),
            Text(
              '右摇杆Y: $_rightY',
            ),
            Text(
              '左按键: $_leftButtonPressed 次',
            ),
            Text(
              '右按键: $_rightButtonPressed 次',
            ),
          ],
        ),
      ),
    );
  }
}
