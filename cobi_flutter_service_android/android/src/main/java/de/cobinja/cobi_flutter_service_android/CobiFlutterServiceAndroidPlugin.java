package de.cobinja.cobi_flutter_service_android;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.embedding.engine.plugins.service.ServiceAware;
import io.flutter.embedding.engine.plugins.service.ServicePluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.JSONMethodCodec;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;

/** CobiFlutterServiceAndroidPlugin */
public class CobiFlutterServiceAndroidPlugin extends BroadcastReceiver
                                                       implements
                                                         FlutterPlugin,
                                                         MethodCallHandler,
                                                         ServiceAware,
                                                         ActivityAware,
                                                         EventChannel.StreamHandler {
  private static final String TAG = "CobiBackgroundPlugin";
  private MethodChannel methodChannel;
  private EventChannel eventChannel;
  private EventChannel.EventSink eventSink = null;
  PluginRegistry.Registrar registrar;
  private FlutterAssets flutterAssets = null;
  
  private Context context;
  
  private CobiFlutterService service;
  private Activity activity;
  
  private static final List<CobiFlutterServiceAndroidPlugin> plugins = new ArrayList<>();
  
  public static void registerWith(PluginRegistry.Registrar registrar) {
    final CobiFlutterServiceAndroidPlugin plugin = new CobiFlutterServiceAndroidPlugin();
    plugin.setupChannel(registrar.messenger(), registrar.context().getApplicationContext());
    plugin.registrar = registrar;
  }
  
  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    setupChannel(binding.getBinaryMessenger(), binding.getApplicationContext());
    flutterAssets = binding.getFlutterAssets();
  }
  
  private void setupChannel(BinaryMessenger messenger, Context context) {
    methodChannel = new MethodChannel(messenger, "de.cobinja/BackgroundPlugin", JSONMethodCodec.INSTANCE);
    methodChannel.setMethodCallHandler(this);
  
    eventChannel = new EventChannel(messenger, "de.cobinja/BackgroundPluginEvents", JSONMethodCodec.INSTANCE);
    eventChannel.setStreamHandler(this);
  
    this.context = context.getApplicationContext();
  
    LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this.context);
    localBroadcastManager.registerReceiver(this, new IntentFilter("de.cobinja/BackgroundPlugin"));
  
    plugins.add(this);
  }
  
  private static CobiFlutterServiceAndroidPlugin getPluginFromService(CobiFlutterService service) {
    CobiFlutterServiceAndroidPlugin result = null;
    for (CobiFlutterServiceAndroidPlugin plugin : plugins) {
      if (plugin.service != null) {
        return plugin;
      }
    }
    for (CobiFlutterServiceAndroidPlugin plugin : plugins) {
      if (plugin != null) {
        return plugin;
      }
    }
    return null;
  }
  
  private static String loadAssetKey(@NonNull CobiFlutterServiceAndroidPlugin plugin, String key) {
    if (plugin.flutterAssets != null) {
      return plugin.flutterAssets.getAssetFilePathByName(key);
    }
    else if (plugin.registrar != null) {
      return plugin.registrar.lookupKeyForAsset(key);
    }
    return null;
  }
  
  public static Bitmap loadAssetBitmap(CobiFlutterService service, String name) {
    CobiFlutterServiceAndroidPlugin plugin = getPluginFromService(service);
    if (plugin == null) {
      return null;
    }
    
    String key = loadAssetKey(plugin, name);
    if (key == null) {
      return null;
    }
  
    AssetManager assetManager = plugin.context.getAssets();
    try {
      String[] assets = assetManager.list("flutter_assets/images");
      StringBuilder buf = new StringBuilder();
      for (String str : assets) {
        buf.append(str).append(", ");
      }
      InputStream stream = assetManager.open(key);
      Bitmap bitmap = BitmapFactory.decodeStream(stream);
      stream.close();
      return bitmap;
    }
    catch (IOException e) {
      Log.e(TAG, "loadAsset: " + e.getMessage(), e);
    }
    return null;
  }
  
  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    try {
      String method = call.method;
      JSONObject args = (JSONObject) call.arguments;
      CobiFlutterService service = null;
      JSONObject notificationData = null;
      
      switch (method) {
        case "startService":
          CobiFlutterService.start(context);
          result.success(true);
          break;
        case "isServiceRunning":
          result.success(CobiFlutterService.isRunning(context));
          break;
        case "stopService":
          CobiFlutterService.stop(context);
          result.success(true);
          break;
        case "sendData":
          service = getServiceFromPlugins();
          if (service == null) {
            result.success(false);
            break;
          }
          
          Object data = args.get("data");
          service.onReceiveData(data);
          result.success(true);
          break;
        case "executeAction":
          service = getServiceFromPlugins();
          
          if (service == null) {
            result.success(false);
            break;
          }
  
          String action = args.getString("action");
          service.executeAction(action);
          result.success(true);
          break;
        case "setNotificationData":
          service = getServiceFromPlugins();
          
          if (service == null) {
            result.success(false);
            break;
          }
          
          notificationData = args.getJSONObject("notificationData");
          SharedPreferences prefs = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE);
          prefs.edit().putString("flutter.cobiFlutterService.notificationData", notificationData.toString()).apply();
          
          result.success(service.setForegroundMode(true));
          break;
        case "setForegroundMode":
          service = getServiceFromPlugins();
  
          if (service == null) {
            result.success(false);
            break;
          }
          
          boolean foreground = args.getBoolean("foreground");
          result.success(service.setForegroundMode(foreground));
          break;
        case "getForegroundMode":
          service = getServiceFromPlugins();
  
          if (service == null) {
            result.success(false);
            break;
          }
          
          result.success(service.getForegroundMode());
          break;
        default:
          result.notImplemented();
      }
    }
    catch (JSONException e){
      Log.e(TAG, "onMethodCall: " + e.getMessage(), e);
      result.error("100", "Failed to read arguments", e.toString());
    }
  }
  
  private static CobiFlutterService getServiceFromPlugins() {
    for (CobiFlutterServiceAndroidPlugin plugin: plugins) {
      if (plugin.service != null) {
        return plugin.service;
      }
    }
    
    return null;
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    methodChannel.setMethodCallHandler(null);
    eventChannel.setStreamHandler(null);
    plugins.remove(this);
    registrar = null;
  }
  
  @Override
  public void onAttachedToService(@NonNull ServicePluginBinding binding) {
    service = (CobiFlutterService) binding.getService();
  }
  
  @Override
  public void onDetachedFromService() {
    service = null;
  }
  
  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    activity = binding.getActivity();
  }
  
  @Override
  public void onDetachedFromActivityForConfigChanges() {
    activity = null;
  }
  
  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    activity = binding.getActivity();
  }
  
  @Override
  public void onDetachedFromActivity() {
    activity = null;
  }
  
  // used for communication from service to plugin
  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent == null) {
      return;
    }
    
    String action = intent.getStringExtra("action");
    
    switch(action) {
      case "onServiceStopped":
        if (eventSink != null) {
          eventSink.success(false);
        }
        break;
      case "onServiceStarted":
        if (eventSink != null) {
          eventSink.success(true);
        }
        break;
      case "onReceiveData":
        String data = intent.getStringExtra("data");
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("data", data);
        methodChannel.invokeMethod("onReceiveData", arguments);
        break;
      default:
        break;
    }
  }
  //Stream handling for EventChannel
  @Override
  public void onListen(Object arguments, EventChannel.EventSink events) {
    eventSink = events;
    events.success(CobiFlutterService.isRunning(context));
  }
  
  @Override
  public void onCancel(Object arguments) {
    eventSink.endOfStream();
  }
}
