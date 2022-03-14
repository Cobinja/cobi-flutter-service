import 'dart:async';
import 'dart:io';

import 'package:cobi_flutter_service_android/cobi_flutter_service_android.dart';
import 'package:cobi_flutter_service_platform_interface/cobi_flutter_service_platform_interface.dart';

class CobiFlutterService {
  CobiFlutterService._() {
    if (Platform.isAndroid) {
      MethodChannelImplAndroid.registerWith();
    }
    
    _platform = CobiFlutterServicePlatform.instance;
  }
  
  static CobiFlutterService? _instance;
  
  // This returns a singleton to control the background service
  static CobiFlutterService get instance => _getInstance();
  
  late CobiFlutterServicePlatform _platform;
  
  static CobiFlutterService _getInstance() {
    if (_instance != null) {
      return _instance!;
    }
    
    _instance = CobiFlutterService._();
    return _instance!;
  }
  
  /// Call this first. If you don't, your service will not work.
  /// The callback's receivePort will receive the message 'stop' when the service is to be stopped.
  /// It should send 'stopped' via its sendPort once the work is finished.
  /// It can also send 'not stopped' in order to keep working.
  /// If it does not do so within 5 seconds, the service will be stopped forcefully
  Future<void> initService(CobiFlutterServiceCallback callback, [bool autostartOnBoot = false]) {
    return _platform.initService(callback, autostartOnBoot);
  }
  
  /// This method sends data to the background isolate
  /// If you want to send data from the background isolate to the UI isolate, please see the included example app
  Future<bool?> sendData(dynamic data) {
    return _platform.sendData(data);
  }
  
  /// This sets the notification data that is displayed in the device's status bar
  Future<bool?> setNotificationData(CobiNotificationData data) {
    return _platform.setNotificationData(data);
  }
  
  Future<bool?> setForegroundMode(bool foreground) {
    return _platform.setForegroundMode(foreground);
  }
  
  Future<bool?> getForegroundMode() {
    return _platform.getForegroundMode();
  }
  
  /// This starts the service
  Future<bool?> startService() {
    return _platform.startService();
  }
  
  /// This stops the service on the platform side when the background isolate reports that it's done cleaning up.
  /// Please see the included example.
  Future<bool?> stopService() {
    return _platform.stopService();
  }
  
  /// This executes actions on the backgroudn isolate.
  /// Note: the action's name does not have to correspond to action names given to the notification.
  Future<bool?> executeAction(String action) {
    return _platform.executeAction(action);
  }
  
  /// This sets a boolean via SharedPreferences whether or not to start the service on device boot.
  /// The value can be changed from the app using the key 'autostartOnBoot' via the SharedPreferences plugin.
  Future<bool> setAutostartOnBoot(value) {
    return _platform.setAutostartOnBoot(value);
  }
  
  /// This reports whether or not the service is to be started at boot
  Future<bool> getAutostartOnBoot() {
    return _platform.getAutostartOnBoot();
  }
  
  /// Listeners get data from the background isolate
  Stream<dynamic> get onDataReceived => _platform.onDataReceived;
  
  /// Listeners are informed when the service changes its state (running or not)
  Stream<bool?> get onServiceStateChanged => _platform.onServiceStateChanged;
}
