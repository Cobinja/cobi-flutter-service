package de.cobinja.cobi_flutter_service_android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
  
  private static final String TAG = "BroadcastReceiver";
  
  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
  
    if (Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_REBOOT.equals(action)) {
      SharedPreferences prefs = context.getApplicationContext().getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE);
      
      if (!prefs.getBoolean("flutter.cobiFlutterService.autostartOnBoot", false)) {
        return;
      }
      
      try {
        CobiFlutterService.start(context);
      }
      catch (Exception e) {
        Log.e(TAG, "onReceive: ", e);
      }
    }
  }
}
