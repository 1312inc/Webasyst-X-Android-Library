# Webasyst-X-Android-Library

## Creating new Webasyst Android client app

### Starting from template

Boilerplate demo app using this Webasyst library is available here: **https://github.com/1312inc/Webasyst-X-Android**

### Starting from scratch

1. Add dependencies

In your application module

```groovy
repositories {
    // Make sure maven central repository is enabled
    mavenCentral()
    // ...
}

dependencies {
    // Library version to use
    def webasyst_version = '1.1.1'
    // Authentication module. Used in log in process and into the process of merging WAID profiles.
    implementation "com.webasyst:auth-kt:$webasyst_version"
    // Webasyst ID (WAID) api client
    implementation "com.webasyst:waid:$webasyst_version"
    // Webasyst client - used to retrieve basic installation data
    implementation "com.webasyst:webasyst:$webasyst_version"
    // Installer client - used to install products
    implementation "com.webasyst:app-installer:$webasyst_version"
    // Required application-specific modules
    implementation "com.webasyst:app-blog:$webasyst_version"
    implementation "com.webasyst:app-shop:$webasyst_version"
    implementation "com.webasyst:app-site:$webasyst_version"
}
```

2. In your app's `AndroidManifest.xml`, in `application` section, add authentication redirect activity.
Note the comment on `<data android:scheme=` key
```xml
<activity android:name="net.openid.appauth.RedirectUriReceiverActivity">
  <intent-filter>
    <action android:name="android.intent.action.VIEW"/>
    <category android:name="android.intent.category.DEFAULT"/>
    <category android:name="android.intent.category.BROWSABLE"/>
    <!-- Authentication redirect scheme. It should be unique across the device. It's recommended to use app's package name. -->
    <data android:scheme="webasyst-x"/>
  </intent-filter>
</activity>
```

3. Configure Webasyst ID (WAID) authentication sevice. This should be done once, preferably early in application's lifecycle. The recommended option is to extend `Application` class and do configuration in it's `onCreate()` method.
See `WebasystAuthService.configure()` for details. For Kotlin, use `configureWebasystAuth()`

4. Implement Authentication Activity.

The easiest way to do it is to extend your Activity from `WebasystAuthActivity` and call it's `waSignIn()` from your SignIn button's `onClick()` callback.

If that's not an option (eg. your Activity is an extension of some other activity) you can use WebasystAuthHelper directly. See `WebasystAuthActivity`'s code for details.

5. Use Webasyst ID (WAID) API methods

Create an instance of the `WAIDClient` to use API methods. Please refer to the [official Webasyst documentation](https://developers.webasyst.com/docs/features/webasyst-id-auth/resources-server-api/)

Get your Webasyst ID auth client id here: https://www.webasyst.com/my/waid/apps/

6. You are good to go.
