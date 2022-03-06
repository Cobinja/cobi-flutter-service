package de.cobinja.cobi_flutter_service_android;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class Utils {
  public static class Icon {
  
    private static final String TAG = "Utils.Icon";
    
    String name;
    String type;
    String subtype;
    
    Icon() {
      this("ic_launcher", "native", "mipmap");
    }
    
    Icon(String name, String subtype) {
      this(name, "native", subtype);
    }
    
    Icon(String name, String type, String subtype) {
      this.name = name;
      this.type = type;
      this.subtype = subtype;
    }
    
    static Icon fromJSON(JSONObject iconData) {
      if (iconData == null) {
        return null;
      }
      try {
        String name = iconData.getString("name");
        String type = iconData.getString("type"); 
        String subtype = iconData.getString("subtype"); 
        return new Icon(name, type, subtype);
      }
      catch (JSONException e) {
        Log.e(TAG, "fromJSON: Could not load icon data", e);
      }
      return null;
    }
    
    Bitmap getIconImage() {
      return null;
    }
    
    int getReosurceId(Context context) {
      if (!"native".equals(type)) {
        return 0;
      }
      Context appContext = context.getApplicationContext();
      int id = appContext.getResources().getIdentifier(name, type, appContext.getPackageName());
      if (id == 0) {
        id = appContext.getApplicationInfo().icon;
      }
      return id;
    }
  } 
}
