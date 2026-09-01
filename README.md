# Shizuku Gamepad Mapper

A Kotlin/Jetpack Compose starter project for selecting a touch or Shizuku-backed input engine, persisting mapping preferences with Preferences DataStore, and starting a floating mapping overlay.

## Build

Open the project in Android Studio with an Android SDK that includes API 35, then sync Gradle and run the `app` configuration on an Android 6.0+ device. The Shizuku provider requires Shizuku or Sui to be installed and started on the device.

## Architecture

`MainActivity` hosts the Compose onboarding and engine-selection UI. `MappingRepository` persists the selected engine, overlay state, dead zone, and sensitivity. `JoystickProcessor` applies dead-zone remapping and sensitivity scaling. `MappingOverlayService` owns the WindowManager overlay. `ShizukuInjector` is an adapter boundary for the generated User Service Binder contract.

## Important process boundary

`IMapperUserService.aidl` defines `injectDown`, `injectMove`, `injectUp`, and `destroy`. `ShizukuUserServiceConnection` binds `MapperUserService` with `Shizuku.UserServiceArgs`, and `ShizukuInjector` forwards events through the generated AIDL proxy. `MapperUserService` runs in the isolated Shizuku User Service process, reflects into `InputManager.injectInputEvent()`, and calls `System.exit(0)` from its remote `destroy()` implementation. The AIDL compiler assigns the `destroy()` method its generated transaction code; the app should call the typed proxy method rather than manually issuing a raw numeric `transact`. If the surrounding protocol specifically requires `16777115`, that number must be implemented in a separately versioned Binder contract or custom `onTransact` path without bypassing the generated AIDL dispatch.

## IPC files

The AIDL source is located at `app/src/main/aidl/com/example/gamepadmapper/IMapperUserService.aidl`. Android Gradle Plugin generates `IMapperUserService.Stub` and `IMapperUserService.Stub.asInterface(IBinder)` during the build. The main process binds the User Service; the remote process owns the privileged `InputManager` reflection and process teardown.

## Permissions and platform behavior

The manifest requests `SYSTEM_ALERT_WINDOW` and `BLUETOOTH_CONNECT`. Overlay access is still granted by the user through system settings. On Android 12 and later, Bluetooth operations require the appropriate runtime permission flow before accessing connected devices. The sample does not include a game-specific mapping editor or a concrete privileged touch-injection protocol; those should be added behind the interfaces in `engine/`.
