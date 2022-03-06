package de.cobinja.cobi_flutter_service_android;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import io.flutter.FlutterInjector;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.dart.DartExecutor;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.JSONMethodCodec;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.view.FlutterCallbackInformation;

public class CobiFlutterService extends Service implements MethodChannel.MethodCallHandler {
  
  private static final String TAG = "CobiFlutterService";
  
  private static final Object syncObject = new Object();
  
  private static final String CHANNEL = "de.cobinja/FlutterService";
  private static final String NOTIFICATION_CHANNEL = "de.cobinja/CobiFlutterBackgroundNotificationMessages";
  
  public static final String ACTION_SHUTDOWN = "ACTION_SHUTDOWN";
  public static final String ACTION_EXECUTE_ACTION = "EXECUTE_FUNCTION";
  
  private MethodChannel channel = null;
  private FlutterEngine engine = null;
  
  private LocalBroadcastManager localBroadcastManager;
  
  private String notifTitle = "Background Service";
  private String notifSubtitle = "Running …";
  private boolean notifShowQuitAction = false;
  private String notifQuitActionCaption = "Quit";
  private IconCompat notifIcon = null;
  Map<String, String> notifActions = new HashMap<>();
  
  boolean foreground = false;
  
  Random rand = new Random();
  
  @Override
  public void onCreate() {
    super.onCreate();
    
    createNotificationChannel();
  }
  
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
  
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    super.onStartCommand(intent, flags, startId);
    
    if (intent == null) {
      return START_NOT_STICKY;
    }
    
    if (ACTION_SHUTDOWN.equals(intent.getAction())) {
      CobiFlutterService.stop(this);
      return START_STICKY;
    }
    
    if (ACTION_EXECUTE_ACTION.equals(intent.getAction())) {
      String action = intent.getStringExtra("action");
      Log.d(TAG, "onStartCommand: executing action: " + action);
      executeAction(action);
      return START_STICKY;
    }
    
    synchronized (syncObject) {
      if (engine == null) {
        runService();
      }
      return START_STICKY;
    }
  }
  
  private void runService() {
    SharedPreferences prefs = getApplicationContext().getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE);
    
    long isolateHandle = prefs.getLong("flutter.cobiFlutterService.isolateHandle", 0);
    long callbackHandle = prefs.getLong("flutter.cobiFlutterService.callbackHandle", 0);
  
    if (isolateHandle == 0 || callbackHandle == 0) {
      if (isolateHandle == 0) {
        Log.d(TAG, "runService: isolateHandle not found");
      }
  
      if (callbackHandle == 0) {
        Log.d(TAG, "runService: callbackHandle not found");
      }
      return;
    }
    
    if (engine != null && engine.getDartExecutor().isExecutingDart()) {
      Log.d(TAG, "Background service already running");
      return;
    }
    
//    startNotification();
    
    FlutterInjector.instance().flutterLoader().startInitialization(getApplicationContext());
    FlutterInjector.instance().flutterLoader().ensureInitializationComplete(getApplicationContext(), null);
    FlutterCallbackInformation callback = FlutterCallbackInformation.lookupCallbackInformation(isolateHandle);
    
    engine = new FlutterEngine(getApplicationContext());
    engine.getServiceControlSurface().attachToService(this, null, true);
    
    BinaryMessenger binaryMessenger = engine.getDartExecutor().getBinaryMessenger();
    channel = new MethodChannel(binaryMessenger, CHANNEL, JSONMethodCodec.INSTANCE);
    channel.setMethodCallHandler(this);
    
    Log.d(TAG, "runService: about to invoke isolate callback");
    DartExecutor.DartCallback dartCallback = new DartExecutor.DartCallback(getAssets(), FlutterInjector.instance().flutterLoader().findAppBundlePath(), callback);
    engine.getDartExecutor().executeDartCallback(dartCallback);
    Log.d(TAG, "runService: invoked isolate callback");
  
    localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
    Intent intent = new Intent("de.cobinja/BackgroundPlugin");
    intent.putExtra("action", "onServiceStarted");
    localBroadcastManager.sendBroadcast(intent);
    Log.d(TAG, "runService: notified starting isolate that the service was started");
  }
  
  private void createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL, NOTIFICATION_CHANNEL, NotificationManager.IMPORTANCE_LOW);
      NotificationManager manager = getSystemService(NotificationManager.class);
      manager.createNotificationChannel(notificationChannel);
    }
  }
  
  private boolean loadNotificationData() {
    SharedPreferences prefs = getApplicationContext().getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE);
    
    String notificationData = prefs.getString("flutter.cobiFlutterService.notificationData", null);
    Log.d(TAG, "loadNotificationData: notificationData: " + notificationData);
    if (notificationData == null) {
      return false;
    }
    
    try {
      JSONObject obj = new JSONObject(notificationData);
      
      notifTitle = obj.has("title") ? obj.getString("title") : "";
      notifSubtitle = obj.has("subtitle") ? obj.getString("subtitle") : "";
      
      notifShowQuitAction = obj.has("showQuitAction") && obj.getBoolean("showQuitAction");
      notifQuitActionCaption = obj.has("quitActionCaption") ? obj.getString("quitActionCaption") : "Quit";
      
      if (obj.has("icon")) {
        JSONObject iconData = obj.getJSONObject("icon");
        String iconName = iconData.getString("name");
        String iconType = iconData.getString("type");
        String iconSubtype = iconData.getString("subtype");
        
        if ("native".equals(iconType)) {
          int id = getApplicationContext().getResources().getIdentifier(iconName, iconSubtype, getPackageName());
          if (id == 0) {
            id = getApplicationContext().getApplicationInfo().icon;
          }
          notifIcon = IconCompat.createWithResource(getApplicationContext(), id);
        }
        else if ("asset".equals(iconType)) {
          try {
            notifIcon = IconCompat.createWithBitmap(Objects.requireNonNull(CobiFlutterServiceAndroidPlugin.loadAssetBitmap(this, iconName)));
          } 
          catch (NullPointerException e) {
            Log.e(TAG, "loadNotificationData: Could not load icon", e);
          }
        }
      }
  
      notifActions.clear();
      if (obj.has("actions")) {
        JSONArray actions = obj.getJSONArray("actions");
        for (int i = 0; i < actions.length(); i++) {
          JSONObject action = actions.getJSONObject(i);
          String caption = action.getString("caption");
          String name = action.getString("name");
          notifActions.put(name, caption);
        }
      }
      
      Log.d(TAG, "loadNotificationData: successfully loaded notification data");
      return true;
    }
    catch (JSONException e) {
      Log.e(TAG, "loadNotificationData: Could not load notification data from shared preferences.", e);
      return false;
    }
  }
  
  private Notification getNotification() {
    if (!loadNotificationData()) {
      return null;
    }
  
    NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL)
      .setContentText(notifSubtitle)
      .setContentTitle(notifTitle)
      .setVisibility(NotificationCompat.VISIBILITY_PRIVATE);
  
    if (notifIcon != null && Build.VERSION.SDK_INT >= 23) {
      builder = builder.setSmallIcon(notifIcon);
    }
    
    int mutableFlags = PendingIntent.FLAG_UPDATE_CURRENT;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      mutableFlags |= PendingIntent.FLAG_MUTABLE;
    }
    int immutableFlags = PendingIntent.FLAG_CANCEL_CURRENT;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      immutableFlags |= PendingIntent.FLAG_IMMUTABLE;
    }
  
    if (notifShowQuitAction) {
      Intent shutdownIntent = new Intent(getApplicationContext(), CobiFlutterService.class);
      shutdownIntent.setAction(ACTION_SHUTDOWN);
      PendingIntent pendingShutdownIntent = PendingIntent.getService(getApplicationContext(), 7, shutdownIntent, mutableFlags);
      builder = builder.addAction(0, notifQuitActionCaption, pendingShutdownIntent);
    }
  
    for (Map.Entry<String, String> entry : notifActions.entrySet()) {
      String action = entry.getKey();
      String caption = entry.getValue();
    
      Intent intent = new Intent(getApplicationContext(), CobiFlutterService.class);
      intent.setAction(ACTION_EXECUTE_ACTION);
      intent.putExtra("action", action);
      int code = rand.nextInt();
      PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), code, intent, immutableFlags);
      builder = builder.addAction(0, caption, pendingIntent);
    }
  
    PackageManager pm = getApplicationContext().getPackageManager();
    Intent activityIntent = pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());
    PendingIntent pendingActivityIntent = PendingIntent.getActivity(getApplicationContext(), 23, activityIntent, mutableFlags);
    builder = builder.setContentIntent(pendingActivityIntent);
    return builder.build();
  }
  
  private void removeNotificationData() {
    SharedPreferences prefs = getApplicationContext().getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE);
    prefs.edit().remove("flutter.cobiFlutterService.notificationData").apply();
  }
  
  public boolean setForegroundMode(boolean foreground) {
    if (foreground) {
      Notification notification = getNotification();
      if (notification != null) {
        this.foreground = true;
        startForeground(101, getNotification());
        return true;
      }
      
      this.foreground = false;
      return false;
    }
    else {
      stopForeground(true);
      this.foreground = false;
      return true;
    }
  }
  
  public boolean getForegroundMode() {
    return foreground;
  }
  
  // calls from service dart code
  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
    switch (call.method) {
      case "stopService":
        CobiFlutterService.stop(this);
        break;
      case "runCallback":
        channel.invokeMethod("runCallback", null, new MethodChannel.Result() {
          @Override
          public void success(@Nullable Object result) {
            if (result == Boolean.FALSE) {
              CobiFlutterService.stop(getApplicationContext());
            }
          }
  
          @Override
          public void error(String errorCode, @Nullable String errorMessage, @Nullable Object errorDetails) {
            Log.e(TAG, "error: " + errorCode + ", message: " + errorMessage);
          }
  
          @Override
          public void notImplemented() {
          }
        });
        result.success(null);
        break;
      case "sendData":
        JSONObject data = call.argument("data");
        
        String arg = null;
        if (data != null) {
          arg = data.toString();
        }
        
        Intent intent = new Intent("de.cobinja/BackgroundPlugin");
        intent.putExtra("action", "onReceiveData");
        intent.putExtra("data", arg);
        localBroadcastManager.sendBroadcast(intent);
        break;
      default:
        result.notImplemented();
        break;
    }
  }
  
  public void executeAction(String action) {
    Map<String, String> arguments = new HashMap<>();
    arguments.put("action", action);
    channel.invokeMethod("executeAction", arguments);
  }
  
  // Frontend -> Service
  public void onReceiveData(Object data) {
    Map<String, Object> arguments = new HashMap<>();
    arguments.put("data", data);
    channel.invokeMethod("onReceiveData", arguments);
  }
  
  @Override
  public void onDestroy() {
    Log.d(TAG, "onDestroy: invoked");
    
    if (channel != null) {
      channel.invokeMethod("stopService", null);
    }
  
    foreground = false;
    removeNotificationData();
    
    if (localBroadcastManager != null) {
      Intent intent = new Intent("de.cobinja/BackgroundPlugin");
      intent.putExtra("action", "onServiceStopped");
      localBroadcastManager.sendBroadcast(intent);
    }
    
    if (engine != null) {
      engine.destroy();
      engine = null;
    }
    
    stopForeground(true);
  }
  
  public static void start(@NonNull Context context) {
    Intent intent = new Intent(context.getApplicationContext(), CobiFlutterService.class);
    context.startService(intent);
  }
  
  public static void stop(@NonNull Context context) {
    Intent intent = new Intent(context.getApplicationContext(), CobiFlutterService.class);
    context.stopService(intent);
  }
  
  public static boolean isRunning(Context context) {
    ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
      if (CobiFlutterService.class.getName().equals(service.service.getClassName())) {
        return true;
      }
    }
    return false;
  }
}
