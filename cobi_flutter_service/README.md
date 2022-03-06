# CobiFlutterService plugin

This executes dart code in a second isolate while providing communication between the main and background isolates.

It also integrates with the device's status bar.

This plugin is currently only available for Android.

## Usage

To use this plugin, add cobi_flutter_service as a dependency in your pubspec.yaml file.

Then implement a runner function:
```dart
void serviceRunner(SendPort sendPort, ReceivePort receivePort) {
  print("Service runner executed");
}
```
To prepare your app to run the background service, initialize it:
```dart
void main() {
  WidgetsFlutterBinding.ensureInitialized();
  CobiFlutterService.instance.initService(serviceRunner, true);
  runApp(MyApp());
}
```
Then you can start the service by calling
```dart
CobiFlutterService.instance.startService();
```
For a more advanced use case, see the included example
