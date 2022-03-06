library cobi_flutter_service_common;

import 'dart:isolate';

/// This provides the settings for a notification action to be displayed in the device's status bar
class CobiNotificationAction {
  /// A unique name for  this action
  String name;
  /// The caption used to display this action
  String caption;
  
  CobiNotificationAction({required this.name, required this.caption});
  
  Map<String, dynamic> toMap() {
    return {
      "name": name,
      "caption": caption
    };
  }
}

/// This contains the icon data for the displayed notification
/// On Android, the icon has to available via a resource id
class CobiIconData {
  /// On Android, this contains the resource name with which the icon can be found
  String name;
  /// the icon type, e.g. 'mipmap'
  String? type;
  String? subtype;
  
  CobiIconData({required this.name, this.type, this.subtype});
  
  Map<String, dynamic> toMap() {
    return {
      "name": name,
      "type": type,
      "subtype": subtype
    };
  }
}

/// This contains the data needed to display the foreground notification
class CobiNotificationData {
  /// The notfication's title
  String title;
  /// The notfication's subtitle
  String? subtitle;
  /// The notfication's icon
  CobiIconData? icon;
  /// Whether or not to show a quit-action
  bool? showQuitAction;
  /// the quit action's caption
  String? quitActionCaption;
  /// A list of actions to be displayed
  List<CobiNotificationAction>? actions;
  
  CobiNotificationData({
    required this.title,
    this.subtitle,
    this.icon,
    this.showQuitAction = false,
    this.quitActionCaption,
    this.actions
  });
  
  Map<String, dynamic> toMap() {
    Map<String, dynamic> result = {
      "title": title,
      "showQuitAction": showQuitAction ?? false,
      "quitActionCaption": quitActionCaption ?? "Quit"
    };
    
    if (subtitle != null) {
      result["subtitle"] = subtitle!;
    }
    if (icon != null) {
      result["icon"] = icon!.toMap();
    }
    if (actions != null) {
      result["actions"] = actions!.map((e) => e.toMap()).toList();
    }
    
    return result;
  }
}

/// The type of the callback function that is called when the service is started
typedef CobiFlutterServiceCallback(SendPort sendPort, ReceivePort recPort);
