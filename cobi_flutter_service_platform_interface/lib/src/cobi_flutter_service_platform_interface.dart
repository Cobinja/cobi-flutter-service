import 'dart:ui';

import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'method_channel_impl.dart';
import 'types.dart';

/// The interface that implementations of cobi_flutter_service must implement.
///
/// Platform implementations should extend this class rather than implement it as `cobi_flutter_service`
/// does not consider newly added methods to be breaking changes. Extending this class
/// (using `extends`) ensures that the subclass will get the default implementation, while
/// platform implementations that `implements` this interface will be broken by newly added
/// [CobiFlutterServicePlatform] methods.
abstract class CobiFlutterServicePlatform {
  
  /// The default instance of [CobiFlutterServicePlatform] to use.
  ///
  /// Defaults to [CobiFlutterServiceMethodChannelImplCommon].
  static CobiFlutterServicePlatform get instance => _instance;
  
  /// Platform-specific plugins should set this with their own platform-specific
  /// class that extends [CobiFlutterServicePlatform] when they register themselves.
  static set instance(CobiFlutterServicePlatform value) {
    if (!value.isMock) {
      try {
        value._verifyProvidesDefaultImplementations();
      } on NoSuchMethodError catch (_) {
        throw AssertionError('Platform interfaces must not be implemented with `implements`');
      }
    }
    _instance = value;
  }
  
  static CobiFlutterServicePlatform _instance = CobiFlutterServiceMethodChannelImplCommon();
  
  /// Only mock implementations should set this to true.
  ///
  /// Mockito mocks are implementing this class with `implements` which is forbidden for anything
  /// other than mocks (see class docs). This property provides a backdoor for mockito mocks to
  /// skip the verification that the class isn't implemented with `implements`.
  @visibleForTesting
  bool get isMock => false;
  
  bool _defaultAutostart = false;
  
  @protected
  Future<void> init(Function() isolateCallback, CobiFlutterServiceCallback callback, bool autostartOnBoot) {
    debugPrint("Init cobi_flutter_service");
    _defaultAutostart = autostartOnBoot;
    CallbackHandle isoHandle = PluginUtilities.getCallbackHandle(isolateCallback)!;
    CallbackHandle cbHandle = PluginUtilities.getCallbackHandle(callback)!;
    
    return SharedPreferences.getInstance()
    .then((prefs) async {
      prefs.setInt("cobiFlutterService.isolateHandle", isoHandle.toRawHandle());
      prefs.setInt("cobiFlutterService.callbackHandle", cbHandle.toRawHandle());
      
      // only set autostart value if it doesn't exist yet so that apps can allow setting that value by the user
      bool? currentAutoStart = prefs.getBool("cobiFlutterService.autostartOnBoot");
      if (currentAutoStart == null) {
        await prefs.setBool("cobiFlutterService.autostartOnBoot", autostartOnBoot);
      }
    });
  }
  
  void initService(CobiFlutterServiceCallback callback, bool autostartOnBoot);
  
  @protected
  void setIsolateCallback(Function() isolateCallback) {
    CallbackHandle isoHandle = PluginUtilities.getCallbackHandle(isolateCallback)!;
    
    SharedPreferences.getInstance()
    .then((prefs) => prefs.setInt("cobiFlutterService.isolateHandle", isoHandle.toRawHandle()));
  }
  
  Future<bool?> startService();
  
  Future<bool?> stopService();
  
  Future<bool?> sendData(dynamic data);
  
  Future<bool?> setNotificationData(CobiNotificationData data);
  
  Future<bool?> executeAction(String action);
  
  Future<bool?> isServiceRunning();
  
  Future<bool?> setForegroundMode(bool foreground);
  
  Future<bool?> getForegroundMode();
  
  Future<bool> setAutostartOnBoot(value) async {
    SharedPreferences prefs = await SharedPreferences.getInstance();
    return prefs.setBool("cobiFlutterService.autostartOnBoot", value);
  }
  
  Future<bool> getAutostartOnBoot() async {
    SharedPreferences prefs = await SharedPreferences.getInstance();
    return prefs.getBool("cobiFlutterService.autostartOnBoot") ?? _defaultAutostart;
  }

  Stream<dynamic> get onDataReceived;
  
  Stream<bool?> get onServiceStateChanged;
  
  void _verifyProvidesDefaultImplementations() {}
}
