import 'dart:async';
import 'dart:isolate';
import 'dart:ui';

import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'cobi_flutter_service_platform_interface.dart';
import 'types.dart';

// Background service
void isolateFunctionMethodChannelCommon() {
  WidgetsFlutterBinding.ensureInitialized();
  MethodChannel _methodChannel = new MethodChannel("de.cobinja/FlutterService", JSONMethodCodec());
  ReceivePort? localRecPort;
  SendPort? localSendPort;
  
  void portListener(message) {
    if (message is Map) {
      if (message["action"] != null) {
        if (message["action"] == "sendData") {
          _methodChannel.invokeMethod<bool>("sendData", {"data": message["data"]});
        }
      }
    }
  }
  
  _methodChannel.setMethodCallHandler((call) async {
    if (call.method == "runCallback") {
      SharedPreferences prefs = await SharedPreferences.getInstance();
      int? rawHandle = prefs.getInt("cobiFlutterService.callbackHandle");
      
      if (rawHandle == null) {
        return false;
      }
      
      CallbackHandle cbHandle = CallbackHandle.fromRawHandle(rawHandle);
      Function? callback = PluginUtilities.getCallbackFromHandle(cbHandle);
      
      if (callback == null) {
        return false;
      }
      
      localRecPort = ReceivePort();
      SendPort remoteSendPort = localRecPort!.sendPort;
      localRecPort!.listen(portListener);
      
      ReceivePort remoteRecPort = ReceivePort();
      localSendPort = remoteRecPort.sendPort;
      
      callback(remoteSendPort, remoteRecPort);
      return true;
    }
    
    if (call.method == "stopService") {
      localSendPort?.send("stop");
    }
    
    if (call.method == "executeAction") {
      String action = call.arguments["action"];
      localSendPort?.send({"action": action});
    }
    
    if (call.method == "onReceiveData") {
      if (call.arguments != null && call.arguments["data"] != null)
      localSendPort!.send({
        "event": "onReceiveData",
        "data": call.arguments["data"]
      });
    }
  });
  
  _methodChannel.invokeMethod<bool>("runCallback");
}

// Available for App
class CobiFlutterServiceMethodChannelImplCommon extends CobiFlutterServicePlatform {
  
  MethodChannel _methodChannel = new MethodChannel("de.cobinja/BackgroundPlugin", JSONMethodCodec());
  EventChannel _eventChannel = new EventChannel("de.cobinja/BackgroundPluginEvents", JSONMethodCodec());
  
  CobiFlutterServiceMethodChannelImplCommon() {
    _methodChannel.setMethodCallHandler(_handleMethodCalls);
    _eventChannel.receiveBroadcastStream()
    .listen((event) {
      if (event is bool) {
        _streamControllerServiceRunning.sink.add(event);
      }
    });
    
    _methodChannel.invokeMethod("isServiceRunning")
    .then((value) {
      _streamControllerServiceRunning.sink.add(value);
    });
    
  }
  
  StreamController<dynamic> _streamControllerReceivedData = StreamController.broadcast();
  StreamController<bool?> _streamControllerServiceRunning = StreamController.broadcast();

  Stream<dynamic> get onDataReceived => _streamControllerReceivedData.stream;
  Stream<bool?> get onServiceStateChanged => _streamControllerServiceRunning.stream;
  
  Future<dynamic> _handleMethodCalls(MethodCall call) async {
    if (call.method == "onReceiveData") {
      if (call.arguments != null && call.arguments["data"] != null) {
        _streamControllerReceivedData.sink.add(call.arguments["data"]);
      }
      else {
        debugPrint("onReceiveData: data field empty");
      }
    }
  }
  
  void initService(CobiFlutterServiceCallback callback, bool autostartOnBoot) async {
    await init(isolateFunctionMethodChannelCommon, callback, autostartOnBoot);
    bool? isRunning = await _methodChannel.invokeMethod("isServiceRunning");
    _streamControllerServiceRunning.sink.add(isRunning);
  }

  @override
  Future<bool?> sendData(dynamic data) {
    return _methodChannel.invokeMethod<bool>("sendData", {"data": data});
  }

  @override
  Future<bool?> setNotificationData(CobiNotificationData data) {
    return _methodChannel.invokeMethod<bool>("setNotificationData", {"notificationData": data.toMap()});
  }
  
  @override
  Future<bool?> setForegroundMode(bool foreground) {
    Map<String, bool> args = {
      "foreground": foreground
    };
    return _methodChannel.invokeMethod<bool>("setForegroundMode", args);
  }
  
  @override
  Future<bool?> getForegroundMode() {
    return _methodChannel.invokeMethod("getForegroundMode");
  }

  @override
  Future<bool?> startService() {
    return _methodChannel.invokeMethod<bool>("startService");
  }

  @override
  Future<bool?> stopService() {
    return _methodChannel.invokeMethod<bool>("stopService");
  }

  @override
  Future<bool?> executeAction(String action) {
    Map<String, dynamic> args = {};
    args["action"] = action;
    
    return _methodChannel.invokeMethod<bool>("executeAction", args);
  }

  @override
  Future<bool?> isServiceRunning() async {
    return _methodChannel.invokeMethod<bool>("isServiceRunning");
  }
}
