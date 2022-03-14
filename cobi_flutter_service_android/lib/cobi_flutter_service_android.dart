import 'package:cobi_flutter_service_platform_interface/cobi_flutter_service_platform_interface.dart';
import 'package:flutter/widgets.dart';
import 'package:shared_preferences_android/shared_preferences_android.dart';

void _isolateFunctionAndroid() {
  WidgetsFlutterBinding.ensureInitialized();
  // ensure SharedPreferencesAndroid is registered on the background isolate 
  // see also https://github.com/flutter/flutter/issues/98473
  try {
    SharedPreferencesAndroid.registerWith();
  }
  on NoSuchMethodError {
    // do nothing here, try-catch is only used so that it doesn't crash for SharedPreferencesAndroid < 2.0.11
  }
  isolateFunctionMethodChannelCommon();
}

class MethodChannelImplAndroid extends CobiFlutterServiceMethodChannelImplCommon {
  
  static void registerWith() {
    CobiFlutterServicePlatform.instance = MethodChannelImplAndroid();
  }
  
  @override
  Future<void> initService(CobiFlutterServiceCallback callback, bool autostartOnBoot) {
    return init(_isolateFunctionAndroid, callback, autostartOnBoot);
  }
}
