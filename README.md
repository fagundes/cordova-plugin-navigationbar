# cordova-plugin-navigationbar-color

Cordova plugin for controlling the Android system navigation bar.

## Compatibility

Version 0.2.0 targets modern Cordova Android projects:

| cordova-android | Compile API | Minimum Android API |
| --- | ---: | ---: |
| 10.1.2 | 30 | 22 |
| 11.0.0 | 32 | 22 |
| 12.0.1 | 33 | 24 |
| 13.0.0 | 34 | 24 |
| 14.0.1 | 35 | 24 |
| 15.1.0 | 36 | 24 |

The supported platform range is cordova-android `>=10.1.2 <16.0.0`. Version
`0.1.1` remains available for cordova-android 7–9 and Android API 19–29.

| Feature | Minimum API |
| --- | ---: |
| Show, hide, and color the navigation bar | 22 |
| Use dark navigation bar icons | 26 |
| Use native window-insets control and transparency | 30 |

Some versions in the compatibility matrix are end of life. The package has no
npm runtime or development dependencies, but it cannot make the host Android,
Cordova, Java, Gradle, or WebView toolchain secure.

## Installation

```sh
cordova plugin add cordova-plugin-navigationbar-color@0.2.0
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

Make the navigation bar transparent and extend the WebView behind it on Android
11 or newer:

```xml
<preference name="NavigationBarTransparent" value="true" />
```

## API

### `NavigationBar.backgroundColorByHexString(color, lightNavigationBar, transparentNavigationBar)`

Sets the navigation bar color. Accepted hexadecimal formats are `RGB`, `ARGB`,
`RRGGBB`, and `AARRGGBB`, with or without the leading `#`. Invalid values
are logged and ignored.

The boolean arguments are optional and default to `false`. Transparency is
supported on Android 11 or newer.

```js
NavigationBar.backgroundColorByHexString("#1e88e5");
NavigationBar.backgroundColorByHexString("#fff", true);
NavigationBar.backgroundColorByHexString("#000000", false, true);
```

### `NavigationBar.backgroundColorByName(name, lightNavigationBar, transparentNavigationBar)`

Sets one of the built-in colors: `black`, `darkGray`, `lightGray`, `white`,
`gray`, `red`, `green`, `blue`, `cyan`, `yellow`, `magenta`, `orange`,
`purple`, or `brown`.

```js
NavigationBar.backgroundColorByName("white", true);
NavigationBar.backgroundColorByName("black", false, true);
```

### `NavigationBar.hide()` and `NavigationBar.show()`

Hides the navigation bar in immersive mode or shows it again. The requested
state is reapplied after focus, configuration, and resume events. These methods
do not change the Android status bar.

```js
NavigationBar.hide();
NavigationBar.show();
```

### `NavigationBar.size(success, error)`

Returns the stable navigation bar dimensions and position, including while the
bar is hidden:

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

`width` and `height` use density-independent pixels. `widthInPixels` and
`heightInPixels` use physical pixels. Query the value again after a `resize`
or orientation change.

### `NavigationBar.isVisible`

Contains the native visibility reported at startup and is updated when
`hide()` or `show()` is called. It is a compatibility cache, not a live
query of changes made outside the plugin.

## Development

```sh
npm test
npm audit --audit-level=low
npm run test:package
```

See [RELEASING.md](RELEASING.md) for the complete validation matrix.

## License

Apache-2.0. See [LICENSE](LICENSE).
