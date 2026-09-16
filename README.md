# cordova-plugin-navigationbar-color

Cordova plugin for controlling the Android system navigation bar on legacy
Android applications.

## Compatibility

Version 0.1.1 is the maintained legacy release. It is tested with the following
platform range:

| cordova-android | Android API | Android version |
| --- | --- | --- |
| 7.1.4 | 19-27 | 4.4-8.1 |
| 8.1.0 | 19-28 | 4.4-9 |
| 9.1.0 | 22-29 | 5.1-10 |

Feature availability depends on the device API:

| Feature | Minimum API |
| --- | --- |
| Show or hide the navigation bar | 19 (Android 4.4) |
| Set the navigation bar color | 21 (Android 5.0) |
| Use dark navigation bar icons | 26 (Android 8.0) |
| Use a transparent navigation bar | 30 (Android 11) |

Android 4.4-10 and cordova-android 7-9 are end-of-life platforms. This release
keeps the plugin's own dependency tree free of known vulnerabilities, but it
cannot make an end-of-life Android or Cordova toolchain secure.

## Installation

```sh
cordova plugin add cordova-plugin-navigationbar-color@0.1.1
```

The `NavigationBar` global becomes available after Cordova's `deviceready`
event.

## Preferences

Set the initial navigation bar color in the application's `config.xml`:

```xml
<preference name="NavigationBarBackgroundColor" value="#000000" />
```

Use dark navigation icons on a light background on Android 8.0 or newer:

```xml
<preference name="NavigationBarLight" value="true" />
```

Make the navigation bar transparent and allow the WebView to extend behind it
on Android 11 or newer:

```xml
<preference name="NavigationBarTransparent" value="true" />
```

## API

### `NavigationBar.backgroundColorByHexString(color, lightNavigationBar, transparentNavigationBar)`

Sets the navigation bar color. Accepted hexadecimal formats are `RGB`, `ARGB`,
`RRGGBB`, and `AARRGGBB`, with or without the leading `#`. Short formats are
expanded before being sent to Android. Invalid values are logged and ignored.

The `lightNavigationBar` and `transparentNavigationBar` arguments are optional
and default to `false`. Transparency is supported on Android 11 or newer.

```js
NavigationBar.backgroundColorByHexString("#1e88e5", false);
NavigationBar.backgroundColorByHexString("#fff", true);
NavigationBar.backgroundColorByHexString("#000000", false, true);
```

### `NavigationBar.backgroundColorByName(name, lightNavigationBar, transparentNavigationBar)`

Sets one of the built-in colors: `black`, `darkGray`, `lightGray`, `white`,
`gray`, `red`, `green`, `blue`, `cyan`, `yellow`, `magenta`, `orange`, `purple`,
or `brown`.

The `lightNavigationBar` and `transparentNavigationBar` arguments are optional
and default to `false`.

```js
NavigationBar.backgroundColorByName("white", true);
NavigationBar.backgroundColorByName("black", false, true);
```

### `NavigationBar.hide()` and `NavigationBar.show()`

Hides the navigation bar in immersive mode or shows it again. These methods do
not change the Android status bar.

```js
NavigationBar.hide();
NavigationBar.show();
```

### `NavigationBar.size(success, error)`

Returns the navigation bar dimensions and position. The values can change after
a screen rotation or another window-size change.

```js
NavigationBar.size(function (size) {
    console.log(size.width);
    console.log(size.height);
    console.log(size.widthInPixels);
    console.log(size.heightInPixels);
    console.log(size.position); // "bottom", "left", or "right"
}, function (error) {
    console.error(error);
});
```

The `width` and `height` values use density-independent pixels. The
`widthInPixels` and `heightInPixels` values use physical pixels.

Listen for the `resize` event if the application needs to update these values
after an orientation or window-size change:

```js
window.addEventListener("resize", function () {
    NavigationBar.size(function (size) {
        console.log(size);
    });
});
```

### `NavigationBar.isVisible`

Contains the initial native visibility reported at startup and is updated when
`hide()` or `show()` is called. It is a compatibility cache, not a live query of
changes made outside the plugin.

## Development

The repository has no npm runtime or development dependencies.

```sh
npm test
npm audit --audit-level=low
npm run test:package
```

See [RELEASING.md](RELEASING.md) for the complete validation matrix.

## License

Apache-2.0. See [LICENSE](LICENSE).