# Digitus

Digitus is a library designed to make Fingerprint authentication using Nexus Imprint easier for developers.
On API levels below Marshmallow, it will fall back to a password dialog.

![Art](https://raw.githubusercontent.com/afollestad/digitus/master/art.jpg)

**Note**: this library is powered by [material-dialogs](https://github.com/afollestad/material-dialogs),
depending on this library will automatically depend on Material Dialogs. 

# Gradle Dependency

All the Gradle code below goes in `build.gradle` of your module (e.g. `app`), **not** `build.gradle` in
the base of your project.

First, add JitPack.io to your dependencies:

```Gradle
repositories {
    maven { url "https://jitpack.io" }
}
```

Then, add Digitus to your dependencies:

```Gradle
dependencies {
    compile 'com.afollestad:digitus:0.1.0'
}
```

# Testing Fingerprints from an Emulator

The stock Android emulator allows you to test Nexus Imprint. If you create a Nexus 5X emulator,
you can simulate a finger pressing against the reader using this Terminal command:

```shell
adb -e emu finger touch finger-id
```

*finger-id* would be replaced with a number, e.g. 1, 2, 3, etc.


# Tutorial

1. Initialization and De-initialization
    1. Initialization
    2. Permissions Result
    3. De-initialization
2. Callbacks
    1. Ready
    2. Registration Needed
    3. Validate Password
    
---
    
# Initialization and De-initialization

### Initialization

Before you can do anything with Digitus, you need to initialize it:

```java
Digitus.init(this, getString(R.string.app_name), 6969);
```

The first parameter, where `this` is passed above, must be an `Activity` instance which implements
the `DigitusCallback` interface. 

The second parameter, where the name of the app is passed, is a unique string used as the key name 
for the encryption cipher. Don't worry about what that means too much, it should just be unique for
your app.

The last parameter, is a request code which gets passed back to the Activity later (in the section below).
That should also be a unique integer that you don't use for permission requests elsewhere.

### Permissions Result

On Marshmallow, Digitus will automatically request the `USE_FINGERPRINT` permission from Android for you.
You should still include the permission in your `AndroidManifest.xml` file (like the sample project).

Permission will make this request when you make a call to `init(int, String, int)`. You need to receive
this result by overriding the `onRequestPermissionsResult(int, String[], int[])` method in your `Activity`:

```java
@Override
public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    // Notify Digitus of the result
    Digitus.get().handleResult(requestCode, permissions, grantResults);
}
```

The above implementation of `onRequestPermissionsResult(int, String[], int[])` does what you need 
to do. It notifies Digitus if the user allowed the app to access their fingerprint sensor. If not,
Digitus will fallback to using password authentication.

### De-initialization

De-initialization allows Digitus to help Java recycle resources faster, and avoid memory leaks on Android.

```java
Digitus.deinit();
```

You'll want to call this method when you're done with Digitus. It's good to put it in `onDestroy()`
of your `Activity`, or `onPause()` if you remember to initialize Digitus again in `onResume()`.

---

# Callbacks

Digitus has five callbacks in total, but only three of them really need explaining. The other two signal
when authentication is successful, or when an error occurs.

### Ready

The `onDigitusReady(Digitus)` callback method is invoked when Digitus has finished the 
initialization process. At this point, you are able to begin authentication at any time.

When you want to begin authentication, you use the `beginAuthentication()` method:

```java
@Override
public void onDigitusReady(Digitus digitus) {
    // Digitus is ready for authentication.
    // Here, you could enable UI that starts it, hide progress indicators/dialogs, etc.
    // Otherwise, you could immediately start authentication:
    
    digitus.beginAuthentication();
}
```

If you weren't using `beginAuthentication()` from within a Digitus callback method (e.g. if it was 
in  a button click event), you can do it like this:

```java
Digitus.get().beginAuthentication();
```

### Registration Needed

The `onDigitusRegistrationNeeded(Digitus)` callback method is invoked when Digitus determines
there are no fingerprints registered on the device.

Digitus has a method called `openSecuritySettings()` which allows you to open the System Settings
page where the user can do so, if they choose.

```
@Override
public void onDigitusRegistrationNeeded(Digitus digitus) {
    // Digitus needs you to register fingerprints.
    // Here, you would probably want to notify the user, and wait to invoke the method used below
    // until they agree to it.

    // Opens the security settings page in the System Settings where fingerprints can be added.
    digitus.openSecuritySettings();
}
```
If you weren't using `openSecuritySettings()` from within a Digitus callback method, you can do
it like this:

```java
Digitus.get().openSecuritySettings();
```

### Validate Password

If the user doesn't want to authenticate with their fingerprint, or the device doesn't support 
fingerprints (if it doesn't have a sensor or the API level is too old), Digitus will fallback to
using password authentication.

When it falls back to password authentication, an input field will be shown to the user. When they 
submit, this callback is invoked.

In the callback, your app would need to validate their password (determine if it's correct or not). 
You notify Digitus if it's correct or not using `notifyPasswordValidation(boolean)`.

```java
public void onDigitusValidatePassword(Digitus digitus, String password) {
    // The password is correct if they entered "password" as the password
    digitus.notifyPasswordValidation(password.equals("password"));
}
```

You'd probably want to validate the password with a server or something in most cases. You could
do so on a separate thread, and notify Digitus later (see the sample project for an example). 
Again, there's a way to notify Digitus outside of a Digitus callback method.

```java
Digitus.get().notifyPasswordValidation(boolean);
```